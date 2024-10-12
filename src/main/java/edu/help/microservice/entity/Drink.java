package edu.help.microservice.entity;

import java.math.BigDecimal;

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

    @Column(name = "drink_price")
    private Double drinkPrice;

    @Column(name = "alcohol_content")
    private String alcoholContent;

    @Column(name = "drink_image")
    private String drinkImage;

    @Column(name = "drink_discount")
    private BigDecimal drinkDiscount;

    @Column(name = "drink_tags")
    @Convert(converter = IntegerArrayConverter.class)
    private Integer[] drinkTags;

    // New description column
    @Column(name = "description")
    private String description;

    @Column(name = "point_price")
    private Integer point;


    // Constructor with the new description field
    public Drink(Integer drinkId, Integer barId, String drinkName, Double drinkPrice, String alcoholContent,
                 String drinkImage, BigDecimal drinkDiscount, Integer[] drinkTags, String description, Integer point) {
        this.drinkId = drinkId;
        this.barId = barId;
        this.drinkName = drinkName;
        this.drinkPrice = drinkPrice;
        this.alcoholContent = alcoholContent;
        this.drinkImage = drinkImage;
        this.drinkDiscount = drinkDiscount;
        this.drinkTags = drinkTags;
        this.description = description;
        this.point = point;
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

    public Double getDrinkPrice() {
        return drinkPrice;
    }

    public void setDrinkPrice(Double drinkPrice) {
        this.drinkPrice = drinkPrice;
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

    public BigDecimal getDrinkDiscount() {
        return drinkDiscount;
    }

    public void setDrinkDiscount(BigDecimal drinkDiscount) {
        this.drinkDiscount = drinkDiscount;
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
}