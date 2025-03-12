package edu.help.microservice.dto;

import lombok.Data;

@Data
public class BarRegistrationRequest {
    private String email;
    private String password;
    private String companyName;
    private String companyNickname;
    private String country;
    private String region;
    private String city;
    private String address;
    private String openTime;
    private String closeTime;
}
