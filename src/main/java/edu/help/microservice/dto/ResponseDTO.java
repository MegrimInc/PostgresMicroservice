package edu.help.microservice.dto;

import java.util.List;

public class ResponseDTO {
    private List<MerchantDTO> merchants;
   
    // Getters and Setters
    public List<MerchantDTO> getMerchants() {
        return merchants;
    }

    public void setMerchants(List<MerchantDTO> merchants) {
        this.merchants = merchants;
    }
}