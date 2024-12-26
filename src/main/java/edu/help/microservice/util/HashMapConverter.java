package edu.help.microservice.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class HashMapConverter implements AttributeConverter<Map<String, String>, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, String> map) {
        try {
            String jsonString = map == null ? null : objectMapper.writeValueAsString(map);
            // Add PostgreSQL casting (only needed if necessary)
            String finalString = jsonString == null ? null : jsonString + "::jsonb";

            return finalString;
        } catch (JsonProcessingException e) {
            // Debug log: Error
            System.err.println("Error in convertToDatabaseColumn: " + e.getMessage());
            throw new IllegalArgumentException("Error converting map to JSON string.", e);
        }
    }

    @Override
    public Map<String, String> convertToEntityAttribute(String json) {
        if (json == null) {
            return new HashMap<>();
        }
        try {
            // Convert JSON string to Map
            Map<String, String> map = objectMapper.readValue(json, HashMap.class);
            return map;
        } catch (IOException e) {
            throw new IllegalArgumentException("Error reading JSON string into map.", e);
        }
    }
}
