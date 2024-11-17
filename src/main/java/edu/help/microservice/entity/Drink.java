package edu.help.microservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "drinks")
public class Drink {

    @JsonIgnore
    @Column(name = "bar_id")
    private Integer barId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "drink_id")
    private Integer drinkId;

    @Column(name = "drink_name")
    private String drinkName;

    @Column(name = "alcohol_content")
    private String alcoholContent;

    @Column(name = "drink_image")
    private String drinkImage;

    @Column(name = "drink_tags")
    @Convert(converter = IntegerArrayConverter.class)
    private Integer[] drinkTags;

    // New description column
    @Column(name = "description")
    private String description;

    @Column(name = "point_price")
    private Integer point;

    // New columns for pricing
    @Column(name = "single_price")
    private Double singlePrice;

    @Column(name = "single_happy_price")
    private Double singleHappyPrice;

    @Column(name = "double_price")
    private Double doublePrice;

    @Column(name = "double_happy_price")
    private Double doubleHappyPrice;

    // Constructor with new fields
    public Drink(Integer drinkId, Integer barId, String drinkName, String alcoholContent,
                 String drinkImage, Integer[] drinkTags, String description, Integer point,
                 Double singlePrice, Double singleHappyPrice, Double doublePrice, Double doubleHappyPrice) {
        this.drinkId = drinkId;
        this.barId = barId;
        this.drinkName = drinkName;
        this.alcoholContent = alcoholContent;
        this.drinkImage = drinkImage;
        this.drinkTags = drinkTags;
        this.description = description;
        this.point = point;
        this.singlePrice = singlePrice;
        this.singleHappyPrice = singleHappyPrice;
        this.doublePrice = doublePrice;
        this.doubleHappyPrice = doubleHappyPrice;
    }

    // Default constructor
    public Drink() {}

    // Getters and Setters
    public Integer getDrinkId() {
        return drinkId;
    }

    public void setDrinkId(Integer drinkId) {
        this.drinkId = drinkId;
    }

    public Integer getBarId() {
        return barId;
    }

    public void setBarId(Integer barId) {
        this.barId = barId;
    }

    public String getDrinkName() {
        return drinkName;
    }

    public void setDrinkName(String drinkName) {
        this.drinkName = drinkName;
    }

    public String getAlcoholContent() {
        return alcoholContent;
    }

    public void setAlcoholContent(String alcoholContent) {
        this.alcoholContent = alcoholContent;
    }

    public String getDrinkImage() {
        return drinkImage;
    }

    public void setDrinkImage(String drinkImage) {
        this.drinkImage = drinkImage;
    }


    public Integer[] getDrinkTags() {
        return drinkTags;
    }

    public void setDrinkTags(Integer[] drinkTags) {
        this.drinkTags = drinkTags;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getPoint() {
        return point;
    }

    public void setPoint(Integer point) {
        this.point = point;
    }

    public Double getSinglePrice() {
        return singlePrice;
    }

    public void setSinglePrice(Double singlePrice) {
        this.singlePrice = singlePrice;
    }

    public Double getSingleHappyPrice() {
        return singleHappyPrice;
    }

    public void setSingleHappyPrice(Double singleHappyPrice) {
        this.singleHappyPrice = singleHappyPrice;
    }

    public Double getDoublePrice() {
        return doublePrice;
    }

    public void setDoublePrice(Double doublePrice) {
        this.doublePrice = doublePrice;
    }

    public Double getDoubleHappyPrice() {
        return doubleHappyPrice;
    }

    public void setDoubleHappyPrice(Double doubleHappyPrice) {
        this.doubleHappyPrice = doubleHappyPrice;
    }
}
