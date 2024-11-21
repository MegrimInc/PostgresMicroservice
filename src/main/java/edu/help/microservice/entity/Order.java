package edu.help.microservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Integer orderId;

    @Column(name = "bar_id", nullable = false)
    private Integer barId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "timestamp", nullable = false)
    private ZonedDateTime timestamp;

    @Column(name = "drink_ids", columnDefinition = "jsonb", nullable = false)
    private String drinkIds; // JSON data as String

    @Column(name = "point_price")
    private Integer totalPointPrice;

    @Column(name = "dollar_price")
    private Double totalRegularPrice;

    @Column(name = "tip_amount", columnDefinition = "double precision DEFAULT 0")
    private Double tipAmount;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "station", length = 1)
    private String station;

    @Column(name = "tips_claimed")
    private String tipsClaimed; // Stores the bartender's name or NULL
}
