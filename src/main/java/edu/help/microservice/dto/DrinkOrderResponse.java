package edu.help.microservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrinkOrderResponse {
    private int drinkId;
    private String drinkName;
    private int quantity;
    private String paymentType; // "points" or "regular"
    private String sizeType; // "single" or "double" or empty for drinks without size variations
}
