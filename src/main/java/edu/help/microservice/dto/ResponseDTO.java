package edu.help.microservice.dto;

import java.util.List;

public class ResponseDTO {
    private List<MerchantDto> merchants;
    private List<TagDTO> tags;

    // Getters and Setters
    public List<MerchantDto> getMerchants() {
        return merchants;
    }

    public void setMerchants(List<MerchantDto> merchants) {
        this.merchants = merchants;
    }

    public List<TagDTO> getTags() {
        return tags;
    }

    public void setTags(List<TagDTO> tags) {
        this.tags = tags;
    }
}