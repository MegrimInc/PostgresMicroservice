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
@Table(name = "items")
public class Item {

    @JsonIgnore
    @Column(name = "merchant_id")
    private Integer merchantId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Integer itemId;

    @Column(name = "item_name")
    private String itemName;

    @Column(name = "alcohol_content")
    private String alcoholContent;

    @Column(name = "item_image")
    private String itemImage;

    @Column(name = "item_tags")
    @Convert(converter = IntegerArrayConverter.class)
    private Integer[] itemTags;

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
    public Item(Integer itemId, Integer merchantId, String itemName, String alcoholContent,
                 String itemImage, Integer[] itemTags, String description, Integer point,
                 Double singlePrice, Double singleHappyPrice, Double doublePrice, Double doubleHappyPrice) {
        this.itemId = itemId;
        this.merchantId = merchantId;
        this.itemName = itemName;
        this.alcoholContent = alcoholContent;
        this.itemImage = itemImage;
        this.itemTags = itemTags;
        this.description = description;
        this.point = point;
        this.singlePrice = singlePrice;
        this.singleHappyPrice = singleHappyPrice;
        this.doublePrice = doublePrice;
        this.doubleHappyPrice = doubleHappyPrice;
    }

    // Default constructor
    public Item() {}

    // Getters and Setters
    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public Integer getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Integer merchantId) {
        this.merchantId = merchantId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getAlcoholContent() {
        return alcoholContent;
    }

    public void setAlcoholContent(String alcoholContent) {
        this.alcoholContent = alcoholContent;
    }

    public String getItemImage() {
        return itemImage;
    }

    public void setItemImage(String itemImage) {
        this.itemImage = itemImage;
    }


    public Integer[] getItemTags() {
        return itemTags;
    }

    public void setItemTags(Integer[] itemTags) {
        this.itemTags = itemTags;
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