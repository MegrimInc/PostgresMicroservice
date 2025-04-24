package edu.help.microservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemCountDTO {
    private int itemId;
    private String itemName;
    private double doublePrice;
    private int soldWithDollars;
    private int soldWithPoints;
    private int totalSold;
}