package edu.help.microservice.dto;

import edu.help.microservice.entity.Order.DrinkOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private int barId;
    private int userId;
    private String timestamp; // Receive as String from frontend
    private List<DrinkOrder> drinks;
    private int totalPointPrice;
    private double totalRegularPrice;
    private double tip;
    private boolean inAppPayments;
    private String status;
    private String claimer;
    private boolean pointOfSale;
    // Exclude fields not present in the Order entity
}
