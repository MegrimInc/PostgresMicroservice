package edu.help.microservice.dto;

import java.util.Map;

public class BarDTO {
    private Integer id;
    private String name;
    private String barTag;
    private String address;
    private String tagImage;
    private String barImage;
    private String openHours;
    private Map<String, String> happyHours; // Keep this as a Map

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

    public String getBarTag() {
        return barTag;
    }

    public void setBarTag(String barTag) {
        this.barTag = barTag;
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

    public String getBarImage() {
        return barImage;
    }

    public void setBarImage(String barImage) {
        this.barImage = barImage;
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
