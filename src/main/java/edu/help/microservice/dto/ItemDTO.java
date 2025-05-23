// src/main/java/edu/help/microservice/dto/ItemDTO.java
package edu.help.microservice.dto;

import lombok.*;

// returned to React
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ItemDTO {
    private Integer itemId;
    private String name;
    private String description;
    private Integer pointPrice;
    private Double regularPrice;
    private Double discountPrice;
    private Double taxPercent;
    private Integer[] categoryIds;
     private Double gratuityPercent;
    private String image;


}
