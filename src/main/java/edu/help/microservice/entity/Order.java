package edu.help.microservice.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int orderId; // Unique identifier for each order

    @Column(nullable = false)
    private int barId; // ID of the bar where the order was placed

    @Column(nullable = false)
    private int userId; // ID of the user who placed the order

    @Column(nullable = false)
    private Instant timestamp; // Timestamp when the order was completed

    @Column(columnDefinition = "jsonb", nullable = false)
    @Convert(converter = JsonbConverter.class)
    private List<DrinkOrder> drinks;


    @Column(nullable = false)
    private int totalPointPrice; // Total price in points if used for payment

    @Column(nullable = false)
    private double totalRegularPrice; // Total price in dollars

    @Column(nullable = false)
    private double tip; // Tip amount given by the user for the order


    @Column(nullable = false)
    private boolean inAppPayments; // Indicates if the payment was made in-app

    @Column(length = 20, nullable = false)
    private String status; // Final status of the order ('claimed', 'delivered', 'canceled')

    @Column(length = 1)
    private String station; // Bartender station identifier (A-Z)

    @Column(length = 255)
    private String tipsClaimed; // "NULL" (as a string) if not claimed, or the bartender's name

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DrinkOrder {
        private int drinkId;
        private String drinkName;
        private String paymentType;
        private String sizeType;
        private int quantity;
    }

    /**
     * Converter for handling JSONB fields in PostgreSQL.
     */
    @Converter
    public static class JsonbConverter implements AttributeConverter<List<DrinkOrder>, String> {
        private final ObjectMapper objectMapper = new ObjectMapper();

        @Override
        public String convertToDatabaseColumn(List<DrinkOrder> attribute) {
            try {
                String jsonString = objectMapper.writeValueAsString(attribute);
                System.out.println("Serialized JSON: " + jsonString); // Debugging line
                return jsonString;
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to convert list to JSON string", e);
            }
        }


        @Override
        public List<DrinkOrder> convertToEntityAttribute(String dbData) {
            try {
                return objectMapper.readValue(dbData, objectMapper.getTypeFactory().constructCollectionType(List.class, DrinkOrder.class));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to convert JSON string to list", e);
            }
        }
    }
}
