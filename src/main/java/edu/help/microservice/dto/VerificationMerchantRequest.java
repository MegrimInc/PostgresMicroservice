package edu.help.microservice.dto;



import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerificationMerchantRequest {
    
    private String verificationCode;
    private String email;
    private String companyName;
    private String companyNickname;
    private String password;
    private String country;
    private String stateOrProvince;
    private String city;
    private String postalCode;
    private String address;
    
}