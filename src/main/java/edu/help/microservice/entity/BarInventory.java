package edu.help.microservice.entity;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "bar_inventory")
public class BarInventory implements Serializable {

    @Id
    @Column(name = "bar_id", nullable = false)
    private Integer barId;

    @Id
    @Column(name = "drink_id", nullable = false)
    private Integer drinkId;

    @Id
    @Column(name = "category_id", nullable = false)
    private Integer categoryId;

    // Getters and Setters
    public Integer getBarId() {
        return barId;
    }

    public void setBarId(Integer barId) {
        this.barId = barId;
    }

    public Integer getDrinkId() {
        return drinkId;
    }

    public void setDrinkId(Integer drinkId) {
        this.drinkId = drinkId;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }
}
