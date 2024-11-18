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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
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

    @Column(name = "station")
    private Character station;

    @Column(name = "tip_amount")
    private Double tipAmount;

    @Column(name = "tips_claimed", nullable = true)
    private String tipsClaimed; // Name of the bartender who claimed the tips
}
