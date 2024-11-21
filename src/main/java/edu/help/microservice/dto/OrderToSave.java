package edu.help.microservice.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderToSave {

    @JsonProperty("barId")
    private int barId;

    @JsonProperty("userId")
    private int userId;

    @JsonProperty("dollarPrice")
    private double dollarPrice;

    @JsonProperty("pointPrice")
    private int pointPrice;

    @JsonProperty("tipAmount")
    private double tipAmount;

    @JsonProperty("drinkIds")
    private List<DrinkOrder> drinkIds; // JSONB -> list of drink objects

    @JsonProperty("status")
    private String status;

    @JsonProperty("claimer")
    private String claimer;

    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("sessionId")
    private String sessionId;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DrinkOrder {

        @JsonProperty("id")
        private int id;            // Drink ID

        @JsonProperty("drinkName")
        private String drinkName;  // Name of the drink

        @JsonProperty("quantity")
        private int quantity;      // Quantity ordered
    }
}
