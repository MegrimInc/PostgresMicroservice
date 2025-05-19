// src/main/java/edu/help/microservice/dto/UpdateItemRequestDTO.java
package edu.help.microservice.dto;

import lombok.*;

// PATCH
@Data
public class UpdateItemRequestDTO {
    private String name;
    private String description;
    private Integer pointPrice;
    private Double regularPrice;
    private Double discountPrice;
    private Double taxPercent;
    private Integer[] categoryIds;
    private String imageUrl;
    private Double gratuityPercent;
}
