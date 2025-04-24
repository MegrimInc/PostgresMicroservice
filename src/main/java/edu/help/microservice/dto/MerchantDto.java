package edu.help.microservice.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class MerchantDto {
    private Integer id;
    private String name;
    private String merchantTag;
    private String address;
    private String tagImage;
    private String merchantImage;
    private String openHours;
    private Map<String, String> happyHours; // Keep this as a Map

    private LocalDateTime startDate;


    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime start_date) {
        this.startDate = start_date;
    }


    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMerchantTag() {
        return merchantTag;
    }

    public void setMerchantTag(String merchantTag) {
        this.merchantTag = merchantTag;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getTagImage() {
        return tagImage;
    }

    public void setTagImage(String tagImage) {
        this.tagImage = tagImage;
    }

    public String getMerchantImage() {
        return merchantImage;
    }

    public void setMerchantImage(String merchantImage) {
        this.merchantImage = merchantImage;
    }

    public String getOpenHours() {
        return openHours;
    }

    public void setOpenHours(String openHours) {
        this.openHours = openHours;
    }

    public Map<String, String> getHappyHours() {
        return happyHours;
    }

    public void setHappyHours(Map<String, String> happyHours) { // Accept a Map as parameter
        this.happyHours = happyHours;
    }
}