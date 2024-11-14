package edu.help.microservice.service;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.SetupIntent;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentMethodListParams;
import com.stripe.param.SetupIntentCreateParams;
import com.stripe.param.TransferCreateParams;

import edu.help.microservice.entity.Bar;
import edu.help.microservice.entity.Customer;
import edu.help.microservice.entity.SignUp;
import edu.help.microservice.exception.BarNotFoundException;
import edu.help.microservice.exception.CustomerNotFoundException;
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

    public void processOrder(double price, double tip, int customerId, int barId) throws StripeException {
        Long priceInCents = Math.round(price * 100);
        Long tipInCents = Math.round(tip * 100);

        var customerOptional = customerRepository.findById(customerId);
        if (customerOptional.isEmpty())
            throw new CustomerNotFoundException(customerId);

        var barOptional = barRepository.findById(barId);
        if (barOptional.isEmpty())
            throw new BarNotFoundException(barId);

        PaymentMethodListParams listParams = PaymentMethodListParams.builder()
                .setCustomer(customerOptional.get().getStripeId())
                .setType(PaymentMethodListParams.Type.CARD) // Specify the type (e.g., CARD)
                .build();

        var paymentMethods = stripeClient.paymentMethods().list(listParams);
        paymentMethods.getData().stream()
                        .forEach(p -> System.out.println(p.toString()));

        chargeCustomer(customerOptional.get(), priceInCents + tipInCents);
        payBar(barOptional.get(), priceInCents + tipInCents);
    }

    public void createStripeCustomer(Customer customer, SignUp signUp) throws StripeException {
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(signUp.getEmail())
                .setName(customer.getFirstName() + " " + customer.getLastName())
                .build();

        var stripeCustomer = stripeClient.customers().create(params);
        customer.setStripeId(stripeCustomer.getId());
    }

    private void chargeCustomer(Customer customer, Long priceInCents) throws StripeException {
        stripeClient.paymentIntents().create(
                new PaymentIntentCreateParams.Builder()
                        .setAmount(priceInCents)
                        .setCurrency(CURRENCY_TYPE)
                        .setCustomer(customer.getStripeId())
                        .setPaymentMethod(customer.getPaymentId())
                        .setConfirm(true)
                        .build());
    }

    private void payBar(Bar bar, Long priceInCents) throws StripeException {
        stripeClient.transfers().create(
                new TransferCreateParams.Builder()
                        .setAmount(priceInCents)
                        .setCurrency(CURRENCY_TYPE)
                        .setDestination(bar.getAccountId())
                        .build());
    }
    
    public Map<String, String> createSetupIntent(int customerId) throws StripeException {
        // Retrieve the customer from the database
        Optional<Customer> customerOpt = customerRepository.findById(customerId);
        if (customerOpt.isEmpty()) {
            throw new CustomerNotFoundException(customerId);
        }

        Customer customer = customerOpt.get();
        String stripeCustomerId = customer.getStripeId();

        // If the customer doesn't have a Stripe ID, create one
        if (stripeCustomerId == null || stripeCustomerId.isEmpty()) {
            // Fetch the associated SignUp record for email
            Optional<SignUp> signUpOpt = signUpRepository.findById(customerId);
            if (signUpOpt.isEmpty()) {
                throw new IllegalStateException("No SignUp record found for customerId: " + customerId);
            }

            SignUp signUp = signUpOpt.get();
            String email = signUp.getEmail();

            // Create the Stripe customer
            var stripeCustomer = stripeClient.customers().create(
                    CustomerCreateParams.builder()
                            .setEmail(email)
                            .setName(customer.getFirstName() + " " + customer.getLastName())
                            .build()
            );

            // Update and save the customer's Stripe ID
            stripeCustomerId = stripeCustomer.getId();
            customer.setStripeId(stripeCustomerId);
            customerRepository.save(customer);
        }

        // Create SetupIntent for the customer
        SetupIntentCreateParams params = SetupIntentCreateParams.builder()
                .setCustomer(stripeCustomerId)
                .build();
        SetupIntent setupIntent = stripeClient.setupIntents().create(params);

        // Return client secret and customer ID for the frontend
        return Map.of(
                "setupIntentClientSecret", setupIntent.getClientSecret(),
                "customerId", stripeCustomerId
        );
    }
}
