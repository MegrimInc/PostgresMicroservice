package edu.help.microservice.config;

public class ApiConfig {
    public static final String ENV = "test"; // Change to "live" when needed
    public static final String BASE_PATH = "/postgres-" + ENV + "-http";
}
