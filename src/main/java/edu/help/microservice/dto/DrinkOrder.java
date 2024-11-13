package edu.help.microservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrinkOrder {
    private int drinkId;
    private int quantity;
    private String paymentType; // "points" or "regular"
    private String sizeType; // "single" or "double" or empty for drinks without size variations
}
