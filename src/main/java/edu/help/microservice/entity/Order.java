package edu.help.microservice.entity;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int orderId; // Unique identifier for each order

    @Column(nullable = false)
    private int barId; // ID of the bar where the order was placed

    @Column(nullable = false)
    private int userId; // ID of the user who placed the order

    @Column(nullable = false)
    private Instant timestamp; // Timestamp when the order was completed

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<DrinkOrder> drinks;

    @Column(nullable = false)
    private int totalPointPrice; // Total price in points if used for payment

    @Column(nullable = false)
    private double totalRegularPrice; // Total price in dollars

    @Column(nullable = false)
    private double tip; // Tip amount given by the user for the order

    @Column(nullable = false)
    private boolean inAppPayments; // Indicates if the payment was made in-app

    @Column(length = 20, nullable = false)
    private String status; // Final status of the order ('claimed', 'delivered', 'canceled')

    @Column(length = 1)
    private String station; // Bartender station identifier (A-Z)

    @Column(length = 255)
    private String tipsClaimed; // "NULL" (as a string) if not claimed, or the bartender's name

    // Inner class for DrinkOrder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DrinkOrder {
        private int drinkId;
        private String drinkName;
        private String paymentType;
        private String sizeType;
        private int quantity;
    }
}
