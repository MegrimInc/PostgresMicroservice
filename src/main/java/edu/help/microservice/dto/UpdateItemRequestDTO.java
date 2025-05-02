package edu.help.microservice.dto;

import lombok.Data;

@Data
public class UpdateItemRequestDTO {
    private String name;
    private String description;
    private Integer pointPrice;
    private Double regularPrice;
    private Double discountPrice;
}
