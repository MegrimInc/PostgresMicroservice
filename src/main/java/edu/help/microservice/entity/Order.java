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
    private int orderId;

    @Column(nullable = false)
    private int barId;

    @Column(nullable = false)
    private int userId;

    @Column(nullable = false)
    private double totalRegularPrice;

    @Column(nullable = false)
    private int totalPointPrice;

    @Column(nullable = false)
    private double tip;

    @Column(nullable = false)
    private boolean inAppPayments;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonbConverter.class) // Use the custom converter
    private List<DrinkOrder> drinks;

    @Column(nullable = false)
    private String status;

    private String claimer;

    @Column(nullable = false)
    private Instant timestamp;

    private String sessionId;

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
                return objectMapper.writeValueAsString(attribute); // Convert List to JSON string
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
