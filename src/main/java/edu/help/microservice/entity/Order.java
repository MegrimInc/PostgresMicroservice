package edu.help.microservice.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
    private int customerId; // ID of the customer who placed the order

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp; // Timestamp when the order was completed

    @JdbcTypeCode(SqlTypes.JSON) 
    @Column(name = "items", columnDefinition = "jsonb", nullable = false)
    private List<ItemOrder> items;

    @Column(name = "total_point_price", nullable = false)
    private int totalPointPrice; // Total price in points if used for payment

    @Column(name = "total_regular_price", nullable = false)
    private double totalRegularPrice; // Total price in dollars

    @Column(name = "total_gratuity", nullable = false)
    private double totalGratuity; // Total gratuity for the order

    @Column(name = "in_app_payments", nullable = false)
    private boolean inAppPayments; // Indicates if the payment was made in-app

    @Column(name = "status", nullable = false)
    private String status; // Final status of the order ('claimed', 'delivered', 'canceled')

    @Column(name = "employee_id")
    private int employeeId; // Station station identifier (A-Z)

    @Column(name = "point_of_sale", nullable = false)
    private String pointOfSale; // Indicates what point of sale the purchase was made from 

    @Column(name = "total_service_fee", nullable = false)
    private double totalServiceFee; // Total service fee charged for the order

    @Column(name = "total_tax", nullable = false)
    private double totalTax; // Total tax charged for the order

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