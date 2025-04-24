package edu.help.microservice.exception;

public class MerchantNotFoundException extends RuntimeException {
  public MerchantNotFoundException(int id) {
    super("Merchant not found with id: " + id);
  }
}