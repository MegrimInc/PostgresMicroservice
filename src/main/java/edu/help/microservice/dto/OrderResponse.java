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
public class OrderResponse {
    private String message;
    private double totalPrice;
    private double tip; // Renamed field
    private List<DrinkOrderResponse> drinks;
    private String messageType;
}
