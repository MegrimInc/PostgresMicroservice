// TipClaimRequest.java
package edu.help.microservice.dto;

import lombok.Data;

@Data
public class TipClaimRequest {
    private int barId;
    private String bartenderName;
    private String bartenderEmail; // Optional
    private String station;        // Bartender's station identifier
}
