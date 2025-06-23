package edu.help.microservice.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.model.SetupIntent;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerUpdateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentMethodListParams;
import com.stripe.param.SetupIntentCreateParams;
import edu.help.microservice.dto.PaymentIdSetRequest;
import edu.help.microservice.entity.Merchant;
import edu.help.microservice.entity.Customer;
import edu.help.microservice.entity.Auth;
import edu.help.microservice.exception.MerchantNotFoundException;
import edu.help.microservice.exception.CustomerNotFoundException;
import edu.help.microservice.exception.CustomerStripeIdNotMachingException;
import edu.help.microservice.exception.InvalidStripeChargeException;
import edu.help.microservice.repository.MerchantRepository;
import edu.help.microservice.repository.CustomerRepository;
import edu.help.microservice.repository.AuthRepository;
import lombok.RequiredArgsConstructor;
import com.stripe.model.Account;
import org.springframework.beans.factory.annotation.Value;



@Service
@RequiredArgsConstructor
public class StripeService {
    private static final String CURRENCY_TYPE = "usd";
    @Value("${stripe.api.key}")
    private String stripeApiKey;
    private final StripeClient stripeClient;
    private final CustomerRepository customerRepository;
    private final MerchantRepository merchantRepository;
    private final AuthRepository authRepository;

    public void processOrder(double finalTotal, int customerId, int merchantId, double totalServiceFee)
            throws StripeException, InvalidStripeChargeException {
        Long finalPriceInCents = Math.round(finalTotal * 100);
        Long serviceFeeInCents = Math.round(totalServiceFee * 100);

        var customerOptional = customerRepository.findById(customerId);
        if (customerOptional.isEmpty())
            throw new CustomerNotFoundException(customerId);

        var merchantOptional = merchantRepository.findById(merchantId);
        if (merchantOptional.isEmpty())
            throw new MerchantNotFoundException(merchantId);

        PaymentMethodListParams.builder()
                .setCustomer(customerOptional.get().getStripeId())
                .setType(PaymentMethodListParams.Type.CARD) // Specify the type (e.g., CARD)
                .build();

        chargeCustomer(merchantOptional.get(), customerOptional.get(), finalPriceInCents, serviceFeeInCents);
    }

    public void createStripeCustomer(Customer customer, Auth signUp) throws StripeException {
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(signUp.getEmail())
                .setName(customer.getFirstName() + " " + customer.getLastName())
                .build();

        var stripeCustomer = stripeClient.customers().create(params);
        customer.setStripeId(stripeCustomer.getId());
    }

    /**
     * Creates a new Stripe Express connected account for the given email.
     * @param email the email address to onboard
     * @return the Stripe account ID
     */
    public String createConnectedAccount(String email) throws StripeException {
        System.out.println("[DEBUG] Creating Stripe account with email: " + email);
        AccountCreateParams accountParams = AccountCreateParams.builder()
                .setType(AccountCreateParams.Type.EXPRESS)
                .setCountry("US")
                .setEmail(email)
                .setCapabilities(
                        AccountCreateParams.Capabilities.builder()
                                .setCardPayments(AccountCreateParams.Capabilities.CardPayments.builder()
                                        .setRequested(true)
                                        .build())
                                .setTransfers(AccountCreateParams.Capabilities.Transfers.builder()
                                        .setRequested(true)
                                        .build())
                                .build())
                .build();

        Account account = stripeClient.accounts().create(accountParams);
        System.out.println("[DEBUG] Created Stripe account: " + account.getId());
        return account.getId();
    }

    private void chargeCustomer(Merchant merchant, Customer customer, Long finalPriceInCents, Long serviceFeeInCents)
            throws StripeException, InvalidStripeChargeException {

        boolean merchantLive = Boolean.TRUE.equals(merchant.getIsLiveAccount());
        boolean customerLive = Boolean.TRUE.equals(customer.getIsLiveAccount());
        boolean paymentLive = Boolean.TRUE.equals(customer.getIsLivePayment());
        boolean isLiveEnv = isLiveMode();

        // Debug logs
        System.out.println("[ENV CHECK] Current Stripe Mode: " + (isLiveEnv ? "LIVE" : "TEST"));
        System.out.println("[ENV CHECK] Merchant.isLiveAccount: " + merchantLive);
        System.out.println("[ENV CHECK] Customer.isLiveAccount: " + customerLive);
        System.out.println("[ENV CHECK] Customer.isLivePayment: " + paymentLive);

        boolean envMatches = merchantLive == isLiveEnv &&
                customerLive == isLiveEnv &&
                paymentLive == isLiveEnv;

        if (!envMatches) {
            System.err.println("[ENV MISMATCH] Environment mismatch detected. Blocking payment.");
            throw new InvalidStripeChargeException(
                    "Environment mismatch: merchant, customer, or payment method is not aligned with current Stripe mode (live/test).",
                    customer);
        }

        PaymentIntent customerCharge = stripeClient.paymentIntents().create(
                new PaymentIntentCreateParams.Builder()
                        .setAmount(finalPriceInCents)
                        .setCurrency(CURRENCY_TYPE)
                        .setCustomer(customer.getStripeId())
                        .setApplicationFeeAmount(serviceFeeInCents)
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
                                        .setDestination(merchant.getAccountId())
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
        boolean isLive = isLiveMode();
        customer.setIsLivePayment(isLive);
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
                Optional<Auth> signUpOpt = authRepository.findById(customerId);
                if (signUpOpt.isEmpty()) {
                    throw new IllegalStateException("No SignUp record found for customerId: " + customerId);
                }

                Auth signUp = signUpOpt.get();
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

    public Map<String, String> getCardDetails(String paymentMethodId) throws StripeException {
        PaymentMethod paymentMethod = stripeClient.paymentMethods().retrieve(paymentMethodId);

        Map<String, String> cardInfo = new HashMap<>();
        cardInfo.put("brand", paymentMethod.getCard().getBrand());
        cardInfo.put("last4", paymentMethod.getCard().getLast4());
        cardInfo.put("exp_month", String.valueOf(paymentMethod.getCard().getExpMonth()));
        cardInfo.put("exp_year", String.valueOf(paymentMethod.getCard().getExpYear()));

        return cardInfo;
    }

    public boolean isLiveMode() {
    return stripeApiKey != null && stripeApiKey.startsWith("sk_live_");
}

}