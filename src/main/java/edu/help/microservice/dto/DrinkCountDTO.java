package edu.help.microservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DrinkCountDTO {
    private int drinkId;
    private String drinkName;
    private double doublePrice;
    private int soldWithDollars;
    private int soldWithPoints;
    private int totalSold;
}
