package edu.help.microservice.exception;

public class BarNotFoundException extends RuntimeException {
  public BarNotFoundException(int id) {
    super("Bar not found with id: " + id);
  }
}
