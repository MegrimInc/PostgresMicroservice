package edu.help.microservice.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

@RestController
@RequestMapping("/stripe")
public class StripeController {

    // Endpoint to create a PaymentIntent
    @PostMapping("/create-payment-intent")
    public Map<String, String> createPaymentIntent(@RequestBody Map<String, Object> paymentData) {
        try {
            // Extract payment details from the request
            int amount = (int) paymentData.get("amount"); // amount in smallest currency unit (e.g., cents)
            String currency = (String) paymentData.getOrDefault("currency", "usd");
            String customerId = (String) paymentData.get("customerId"); // Optional for saved payment methods
            
            // Create PaymentIntent parameters
            PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                    .setAmount((long) amount)
                    .setCurrency(currency);

            if (customerId != null) {
                paramsBuilder.setCustomer(customerId);
            }

            PaymentIntentCreateParams params = paramsBuilder.build();

            // Create the PaymentIntent on Stripe
            PaymentIntent paymentIntent = PaymentIntent.create(params);

            // Return the PaymentIntent client secret to the front end
            Map<String, String> responseData = new HashMap<>();
            responseData.put("clientSecret", paymentIntent.getClientSecret());
            responseData.put("paymentIntentId", paymentIntent.getId());
            return responseData;

        } catch (StripeException e) {
            throw new RuntimeException("Stripe API error: " + e.getMessage());
        }
    }
}