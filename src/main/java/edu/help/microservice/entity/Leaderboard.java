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
    private Integer leaderboardId;

    @Column(nullable = false)
    private Integer merchantId;

    @Column(nullable = false)
    private Integer customerId;

    @Column(nullable = false)
    private Double total;

    @Column(nullable = false)
    private Integer rank;

   @Column(nullable = false)
   private Integer rivalId;
    
   @Column(nullable = false)
    private Double difference;
}