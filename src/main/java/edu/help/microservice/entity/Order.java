package edu.help.microservice.entity;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private int orderId;
    private int barId;
    private int userId;
    private double totalRegularPrice;
    private int totalPointPrice;
    private double tip;
    private boolean inAppPayments;
    private List<DrinkOrder> drinks;
    private String status;
    private String claimer;
    private Instant timestamp; // Changed to Instant for PostgreSQL compatibility
    private String sessionId;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DrinkOrder {
        private int drinkId;
        private String drinkName;  // Added back drinkName
        private String paymentType;  // "points" or "regular"
        private String sizeType;     // "single" or "double"
        private int quantity;
    }
}
