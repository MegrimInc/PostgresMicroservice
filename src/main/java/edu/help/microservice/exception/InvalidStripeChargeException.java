package edu.help.microservice.exception;

import edu.help.microservice.entity.Customer;

public class InvalidStripeChargeException extends Exception {
    public InvalidStripeChargeException(String status, Customer customer) {
        super("Invalid payment\nStatus: " + status + "\nCustomer id: " + customer.getCustomerID());
    }
}

