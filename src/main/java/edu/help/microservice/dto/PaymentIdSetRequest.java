package edu.help.microservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentIdSetRequest {
    private int customerId;
    private String stripeId;
    private String setupIntentId;
}
