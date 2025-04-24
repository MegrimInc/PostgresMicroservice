package edu.help.microservice.entity;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "merchant_inventory")
public class MerchantInventory implements Serializable {

    @Id
    @Column(name = "merchant_id", nullable = false)
    private Integer merchantId;

    @Id
    @Column(name = "item_id", nullable = false)
    private Integer itemId;

    @Id
    @Column(name = "category_id", nullable = false)
    private Integer categoryId;

    // Getters and Setters
    public Integer getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Integer merchantId) {
        this.merchantId = merchantId;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }
}