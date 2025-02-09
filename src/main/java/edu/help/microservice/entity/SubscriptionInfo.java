package edu.help.microservice.entity;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionInfo {
    private LocalDate renewalDate;   // When the subscription expires (or renews)
    private Boolean isSubscribed;    // Whether the customer is subscribed for that bar
    private Integer points;          // Loyalty points (example default: 75)
}