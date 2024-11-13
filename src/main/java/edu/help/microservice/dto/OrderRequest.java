package edu.help.microservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequest {
    private int barId;
    private int userId;
    private double tip; // Renamed field
    private List<DrinkOrder> drinks;
    private boolean inAppPayments;
    private boolean isHappyHour;
}
