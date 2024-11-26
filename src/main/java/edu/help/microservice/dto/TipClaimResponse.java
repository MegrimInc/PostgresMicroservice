// TipClaimResponse.java
package edu.help.microservice.dto;

import edu.help.microservice.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TipClaimResponse {
    private String message;
    private List<Order> orders;

    public TipClaimResponse(String message) {
        this.message = message;
    }
}
