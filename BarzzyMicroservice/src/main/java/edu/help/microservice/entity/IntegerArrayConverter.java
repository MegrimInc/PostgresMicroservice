package edu.help.microservice.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.stream.Collectors;

@Converter
public class IntegerArrayConverter implements AttributeConverter<Integer[], String> {

    @Override
    public String convertToDatabaseColumn(Integer[] attribute) {
        if (attribute == null || attribute.length == 0) {
            return "{}";
        }
        return Arrays.stream(attribute)
                     .map(String::valueOf)
                     .collect(Collectors.joining(",", "{", "}"));
    }

    @Override
    public Integer[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.length() <= 2) {
            return new Integer[0];
        }
        return Arrays.stream(dbData.substring(1, dbData.length() - 1).split(","))
                     .map(Integer::valueOf)
                     .toArray(Integer[]::new);
    }
}
