package edu.help.microservice.dto;

import java.sql.Timestamp;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerificationBarRequest {
    
    private String verificationCode;
    private String email;
    private String companyName;
    private String companyNickname;
    private String password;
    private String country;
    private String region;
    private String city;
    private String postalCode;
    private String address;
    private Timestamp openTime;
    private Timestamp closeTime;
}