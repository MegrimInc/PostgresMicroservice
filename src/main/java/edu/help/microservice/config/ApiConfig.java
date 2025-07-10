package edu.help.microservice.config;

public class ApiConfig {
    public static final String ENV = System.getenv("API_ENV");
    public static final String BASE_PATH = "/postgres-" + ENV + "-http";
}
