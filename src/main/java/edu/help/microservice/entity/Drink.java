package edu.help.microservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;

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

    @Column(name = "solo")
    private String solo;

    @Column(name = "mix")
    private String mix;

    @Column(name = "ss")
    private boolean ss;

    @Column(name = "drink_discount")
    private BigDecimal drinkDiscount;

    @Column(name = "drink_tags")
    @Convert(converter = IntegerArrayConverter.class)
    private Integer[] drinkTags;

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

    public String getSolo() {
        return solo;
    }

    public void setSolo(String solo) {
        this.solo = solo;
    }

    public String getMix() {
        return mix;
    }

    public void setMix(String mix) {
        this.mix = mix;
    }

    public boolean isSs() {
        return ss;
    }

    public void setSs(boolean ss) {
        this.ss = ss;
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
}
