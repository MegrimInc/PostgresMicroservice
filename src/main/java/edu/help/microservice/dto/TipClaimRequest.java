// TipClaimRequest.java
package edu.help.microservice.dto;

import lombok.Data;

@Data
public class TipClaimRequest {
    private int merchantId;
    private String stationName;
    private String stationEmail; // Optional
    private String station;        // Merchanttender's station identifier
}