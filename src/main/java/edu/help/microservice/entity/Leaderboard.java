package edu.help.microservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "leaderboard")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Leaderboard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "leaderboard_id", nullable = false)
    private Integer leaderboardId;

   @Column(name = "merchant_id", nullable = false)
    private Integer merchantId;

   @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    @Column(name = "total", nullable = false)
    private Double total;

    @Column(name = "rank", nullable = false)
    private Integer rank;

   @Column(name = "rival_id", nullable = false)
   private Integer rivalId;
    
   @Column(name = "difference", nullable = false)
    private Double difference;
}