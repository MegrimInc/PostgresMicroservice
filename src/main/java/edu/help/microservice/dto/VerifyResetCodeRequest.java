package edu.help.microservice.dto;

public class VerifyResetCodeRequest {
    private String email;
    private String code;

    // Getters and setters
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
}
