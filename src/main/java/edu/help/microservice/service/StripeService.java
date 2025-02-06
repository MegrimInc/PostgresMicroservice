package edu.help.microservice.service;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.SetupIntent;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerUpdateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentMethodListParams;
import com.stripe.param.SetupIntentCreateParams;
import com.stripe.param.billing.MeterEventCreateParams;

import edu.help.microservice.dto.PaymentIdSetRequest;
import edu.help.microservice.entity.Bar;
import edu.help.microservice.entity.Customer;
import edu.help.microservice.entity.SignUp;
import edu.help.microservice.exception.BarNotFoundException;
import edu.help.microservice.exception.CustomerNotFoundException;
import edu.help.microservice.exception.CustomerStripeIdNotMachingException;
import edu.help.microservice.exception.InvalidStripeChargeException;
import edu.help.microservice.repository.BarRepository;
import edu.help.microservice.repository.CustomerRepository;
import edu.help.microservice.repository.SignUpRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StripeService {
    private static final String CURRENCY_TYPE = "usd";

    private final StripeClient stripeClient;
    private final CustomerRepository customerRepository;
    private final BarRepository barRepository;
    private final SignUpRepository signUpRepository;

    public void processOrder(double price, double tip, int customerId, int barId) throws StripeException, InvalidStripeChargeException {
        Long priceInCents = Math.round(price * 100);
        Long tipInCents = Math.round(tip * 100);

        var customerOptional = customerRepository.findById(customerId);
        if (customerOptional.isEmpty())
            throw new CustomerNotFoundException(customerId);

        var barOptional = barRepository.findById(barId);
        if (barOptional.isEmpty())
            throw new BarNotFoundException(barId);

        PaymentMethodListParams.builder()
                .setCustomer(customerOptional.get().getStripeId())
                .setType(PaymentMethodListParams.Type.CARD) // Specify the type (e.g., CARD)
                .build();

        chargeCustomer(barOptional.get(), customerOptional.get(), priceInCents + tipInCents);
    }

    public void sendMeterEvent(Bar bar) throws StripeException {
        var params = MeterEventCreateParams.builder()
                .setEventName("venue")
                .putPayload("stripe_customer_id", bar.getSubId())
                .build();

        stripeClient.billing().meterEvents().create(params);
    }

    public void createStripeCustomer(Customer customer, SignUp signUp) throws StripeException {
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(signUp.getEmail())
                .setName(customer.getFirstName() + " " + customer.getLastName())
                .build();

        var stripeCustomer = stripeClient.customers().create(params);
        customer.setStripeId(stripeCustomer.getId());
    }

    private void chargeCustomer(Bar bar, Customer customer, Long priceInCents) throws StripeException, InvalidStripeChargeException {
        long fee = Math.round(priceInCents * 0.04) + 60;

        PaymentIntent customerCharge = stripeClient.paymentIntents().create(
                new PaymentIntentCreateParams.Builder()
                        .setAmount(priceInCents)
                        .setCurrency(CURRENCY_TYPE)
                        .setCustomer(customer.getStripeId())
                        .setApplicationFeeAmount(fee)
                        .setAutomaticPaymentMethods(
                                PaymentIntentCreateParams.AutomaticPaymentMethods
                                        .builder()
                                        .setEnabled(true)
                                        .setAllowRedirects(
                                                PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                        .build())
                        .setPaymentMethod(customer.getPaymentId())
                        .setTransferData(
                                PaymentIntentCreateParams.TransferData.builder()
                                        .setDestination(bar.getAccountId())
                                        .build())
                        .setConfirm(true)
                        .build());

        if (!customerCharge.getStatus().equals("succeeded")) {
            throw new InvalidStripeChargeException(customerCharge.getStatus(), customer);
        }
    }

    public void savePaymentId(PaymentIdSetRequest request) throws StripeException {
        System.out.println("Starting save payment id method");

        int customerId = request.getCustomerId();
        var customerOptional = customerRepository.findById(customerId);
        if (customerOptional.isEmpty())
            throw new CustomerNotFoundException(customerId);

        Customer customer = customerOptional.get();
        if (!request.getStripeId().equals(customer.getStripeId()))
            throw new CustomerStripeIdNotMachingException(customerId, request.getStripeId());

        // Use the SetupIntent ID to retrieve the associated payment method
        var setupIntent = stripeClient.setupIntents().retrieve(request.getSetupIntentId());
        String paymentMethodId = setupIntent.getPaymentMethod();

        System.out.println("Payment ID Set: " + paymentMethodId);

        // Set the default payment method
        stripeClient.customers().update(
                request.getStripeId(),
                CustomerUpdateParams.builder()
                        .setInvoiceSettings(
                                CustomerUpdateParams.InvoiceSettings.builder()
                                        .setDefaultPaymentMethod(paymentMethodId)
                                        .build())
                        .build());

        // Save the payment method ID in your database
        customer.setPaymentId(paymentMethodId);
        customerRepository.save(customer);

        System.out.println("Payment method saved and set as default.");
    }

    public Map<String, String> createSetupIntent(int customerId) throws StripeException {
        try {
            // Retrieve the customer from the database
            Optional<Customer> customerOpt = customerRepository.findById(customerId);
            if (customerOpt.isEmpty()) {
                throw new CustomerNotFoundException(customerId);
            }

            Customer customer = customerOpt.get();
            String stripeCustomerId = customer.getStripeId();

            // Log customer information
            System.out.println("Customer retrieved: " + customer);

            // If the customer doesn't have a Stripe ID, create one
            if (stripeCustomerId == null || stripeCustomerId.isEmpty()) {
                System.out.println("No Stripe ID found for customer. Creating a new Stripe customer...");

                // Fetch the associated SignUp record for email
                Optional<SignUp> signUpOpt = signUpRepository.findById(customerId);
                if (signUpOpt.isEmpty()) {
                    throw new IllegalStateException("No SignUp record found for customerId: " + customerId);
                }

                SignUp signUp = signUpOpt.get();
                String email = signUp.getEmail();

                // Log email being used
                System.out.println("Using email for Stripe customer creation: " + email);

                // Create the Stripe customer
                var stripeCustomer = stripeClient.customers().create(
                        CustomerCreateParams.builder()
                                .setEmail(email)
                                .setName(customer.getFirstName() + " " + customer.getLastName())
                                .build());

                // Update and save the customer's Stripe ID
                stripeCustomerId = stripeCustomer.getId();
                customer.setStripeId(stripeCustomerId);
                customerRepository.save(customer);

                // Log Stripe ID creation
                System.out.println("Stripe customer created with ID: " + stripeCustomerId);
            } else {
                System.out.println("Existing Stripe ID found: " + stripeCustomerId);
            }

            // Create SetupIntent for the customer
            System.out.println("Creating SetupIntent for Stripe customer ID: " + stripeCustomerId);
            SetupIntentCreateParams params = SetupIntentCreateParams.builder()
                    .setCustomer(stripeCustomerId)
                    .build();
            SetupIntent setupIntent = stripeClient.setupIntents().create(params);

            // Log the creation of SetupIntent
            System.out.println("SetupIntent created with client secret: " + setupIntent.getClientSecret());

            // Return client secret and customer ID for the frontend
            return Map.of(
                    "setupIntentClientSecret", setupIntent.getClientSecret(),
                    "customerId", stripeCustomerId);
        } catch (StripeException e) {
            // Log Stripe exception details
            System.err.println("Stripe exception occurred: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            // Log general exception details
            System.err.println("Exception occurred: " + e.getMessage());
            throw e;
        }
    }
}
