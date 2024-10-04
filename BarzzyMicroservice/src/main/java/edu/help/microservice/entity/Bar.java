package edu.help.microservice.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "bars")
public class Bar implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bar_id", nullable = false)
    private Integer barId;

    @Column(name = "bar_city", nullable = false, length = 255)
    private String barCity;

    @Column(name = "bar_state", nullable = false, length = 255)
    private String barState;

    @Column(name = "bar_address", nullable = false, length = 255)
    private String barAddress;

    @Column(name = "bar_country", nullable = false, length = 255)
    private String barCountry;

    @Column(name = "tag_image", nullable = false, length = 255)
    private String tagImage;

    @Column(name = "bar_image", nullable = false, length = 255)
    private String barImage;

    @Column(name = "bar_email", nullable = false, length = 255)
    private String barEmail;

    @Column(name = "bar_name", nullable = false, length = 255)
    private String barName;

    @Column(name = "bar_tag", nullable = false, length = 255)
    private String barTag;

    @Column(name = "open_hours", length = 255)
    private String openHours;


    // Getters and Setters
    public Integer getBarId() {
        return barId;
    }

    public void setBarId(Integer barId) {
        this.barId = barId;
    }



    public String getBarCity() {
        return barCity;
    }

    public void setBarCity(String barCity) {
        this.barCity = barCity;
    }

    public String getBarState() {
        return barState;
    }

    public void setBarState(String barState) {
        this.barState = barState;
    }

    public String getBarAddress() {
        return barAddress;
    }

    public void setBarAddress(String barAddress) {
        this.barAddress = barAddress;
    }

    public String getBarCountry() {
        return barCountry;
    }

    public void setBarCountry(String barCountry) {
        this.barCountry = barCountry;
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

    public String getBarEmail() {
        return barEmail;
    }

    public void setBarEmail(String barEmail) {
        this.barEmail = barEmail;
    }

    public String getBarName() {
        return barName;
    }

    public void setBarName(String barName) {
        this.barName = barName;
    }

    public String getBarTag() {
        return barTag;
    }

    public void setBarTag(String barTag) {
        this.barTag = barTag;
    }

    public String getOpenHours() {
        return openHours;
    }

    public void setOpenHours(String openHours) {
        this.openHours = openHours;
    }
}
