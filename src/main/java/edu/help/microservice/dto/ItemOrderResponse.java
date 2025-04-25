package edu.help.microservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemOrderResponse {
    private int itemId;
    private String itemName;
    private int quantity;
    private String paymentType; // "points" or "regular"
}