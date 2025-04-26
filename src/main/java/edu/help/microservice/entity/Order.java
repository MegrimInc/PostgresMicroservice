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
    @Column(name = "order_id")
    private int orderId; // Unique identifier for each order

    @Column(name = "merchant_id", nullable = false)
    private int merchantId; // ID of the merchant where the order was placed

    @Column(name = "customer_id", nullable = false)
    private int customerId; // ID of the user who placed the order

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp; // Timestamp when the order was completed

    @Type(JsonBinaryType.class)
    @Column(name = "items", columnDefinition = "jsonb", nullable = false)
    private List<ItemOrder> items;

    @Column(name = "total_point_price", nullable = false)
    private int totalPointPrice; // Total price in points if used for payment

    @Column(name = "total_regular_price", nullable = false)
    private double totalRegularPrice; // Total price in dollars

    @Column(name = "tip", nullable = false)
    private double tip; // Tip amount given by the user for the order

    @Column(name = "in_app_payments", nullable = false)
    private boolean inAppPayments; // Indicates if the payment was made in-app

    @Column(name = "status", nullable = false)
    private String status; // Final status of the order ('claimed', 'delivered', 'canceled')

    @Column(name = "terminal", length = 1)
    private String terminal; // Station station identifier (A-Z)

    @Column(name = "claimer")
    private String claimer; // "NULL" (as a string) if not claimed, or the station's name

    @Column(name = "point_of_sale", nullable = false)
    private boolean pointOfSale; // Indicates if this is a point of sale purchase or not

    // Inner class for ItemOrder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemOrder {
        private int itemId;
        private String itemName;
        private String paymentType;
        private int quantity;
    }
}