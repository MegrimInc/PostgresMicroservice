package edu.help.microservice.entity;

import java.time.OffsetDateTime;
import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class giOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Integer orderId;

    @Column(name = "bar_id", nullable = false)
    private Integer barId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "timestamp", nullable = false)
    private OffsetDateTime timestamp;

    @ElementCollection
    @Column(name = "drink_ids", nullable = false)
    private List<Integer> drinkIds;

    @Column(name = "point_price")
    private Integer pointPrice;

    @Column(name = "dollar_price")
    private Double dollarPrice;

    @Column(name = "status")
    private String status;

    @Column(name = "claimer")
    private Character claimer;

    @Column(name = "tip_amount")
    private Double tipAmount;

    @Column(name = "tips_claimed", nullable = false)
    private Boolean tipsClaimed = false;

    // Default constructor
    public Order() {}

    // Constructor with all fields
    public Order(Integer barId, Integer userId, OffsetDateTime timestamp, List<Integer> drinkIds, Integer pointPrice,
                 Double dollarPrice, String status, Character claimer, Double tipAmount, Boolean tipsClaimed) {
        this.barId = barId;
        this.userId = userId;
        this.timestamp = timestamp;
        this.drinkIds = drinkIds;
        this.pointPrice = pointPrice;
        this.dollarPrice = dollarPrice;
        this.status = status;
        this.claimer = claimer;
        this.tipAmount = tipAmount;
        this.tipsClaimed = tipsClaimed;
    }

    // Getters and Setters

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public Integer getBarId() {
        return barId;
    }

    public void setBarId(Integer barId) {
        this.barId = barId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public List<Integer> getDrinkIds() {
        return drinkIds;
    }

    public void setDrinkIds(List<Integer> drinkIds) {
        this.drinkIds = drinkIds;
    }

    public Integer getPointPrice() {
        return pointPrice;
    }

    public void setPointPrice(Integer pointPrice) {
        this.pointPrice = pointPrice;
    }

    public Double getDollarPrice() {
        return dollarPrice;
    }

    public void setDollarPrice(Double dollarPrice) {
        this.dollarPrice = dollarPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Character getClaimer() {
        return claimer;
    }

    public void setClaimer(Character claimer) {
        this.claimer = claimer;
    }

    public Double getTipAmount() {
        return tipAmount;
    }

    public void setTipAmount(Double tipAmount) {
        this.tipAmount = tipAmount;
    }

    public Boolean getTipsClaimed() {
        return tipsClaimed;
    }

    public void setTipsClaimed(Boolean tipsClaimed) {
        this.tipsClaimed = tipsClaimed;
    }
}
