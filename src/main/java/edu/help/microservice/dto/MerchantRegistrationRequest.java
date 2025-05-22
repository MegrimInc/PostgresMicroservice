package edu.help.microservice.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantRegistrationRequest {
    private String storeName;
    private String storeNickname;
    private String city;
    private String stateOrProvince;
    private String address;
    private String country;
    private String zipCode;
    private String email;
    private String password;
    private String verificationCode;
}
