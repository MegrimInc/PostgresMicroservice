// TipClaimRequest.java
package edu.help.microservice.dto;

import lombok.Data;

@Data
public class TipClaimRequest {
    private int merchantId;
    private String claimer;
    private String email; // Optional
    private String terminal;        // Station's station identifier
}