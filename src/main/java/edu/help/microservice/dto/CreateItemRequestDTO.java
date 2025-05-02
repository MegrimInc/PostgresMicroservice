package edu.help.microservice.dto;

import lombok.*;

// POST
@Data
public class CreateItemRequestDTO {
    private String name;
    private String description;
    private Integer pointPrice;
    private Double regularPrice;
    private Double discountPrice;
    private Double taxPercent;
}
