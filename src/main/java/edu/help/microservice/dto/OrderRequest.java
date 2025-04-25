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
    private int merchantId;
    private int userId;
    private double tip; // Renamed field
    private List<ItemOrderRequest> items;
    private boolean inAppPayments;
    private boolean discount;
}