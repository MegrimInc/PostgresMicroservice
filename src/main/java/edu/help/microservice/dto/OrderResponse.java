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
    private String name;
    private double totalPrice;
    private int totalPointPrice;
    private double totalGratuity; // Renamed field
    private double totalServiceFee;
    private double totalTax;
    private List<ItemOrderResponse> items;
    private String messageType;
}