package edu.help.microservice.dto;

public class BarDTO {
    private Integer id;
    private String name;
    private String barTag;
    private String address;
    private String tagImage;
    private String barImage;

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
}
