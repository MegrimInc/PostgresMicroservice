package edu.help.microservice.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantDTO {
    private Integer merchantId;
    private String name;
    private String nickname;
    private String city;
    private String zipCode;
    private String stateOrProvince;
    private String country; 
    private String address;
    private String logoImage;
    private String storeImage;
    private Boolean tip;
    private Map<String, String> discountSchedule; // Keep this as a Map
    private Integer bonus;
}
