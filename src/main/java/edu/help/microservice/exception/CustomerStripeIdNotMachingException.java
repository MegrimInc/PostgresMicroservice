package edu.help.microservice.exception;

public class CustomerStripeIdNotMachingException extends RuntimeException {
    public CustomerStripeIdNotMachingException(int customerId, String stripeId) {
        super("The customer [" + customerId + "] does not have stripe id [" + stripeId + "].");
    }
}
