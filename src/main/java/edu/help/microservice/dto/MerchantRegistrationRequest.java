package edu.help.microservice.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantRegistrationRequest {
    private String email;
    private String password;
    private String name;
    private String nickname;
    private String country;
    private String stateOrProvince;
    private String city;
    private String address;
}
