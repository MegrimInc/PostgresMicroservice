// TipClaimResponse.java
package edu.help.microservice.dto;

import edu.help.microservice.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TipClaimResponse {
    private String barEmail;
    private List<Order> orders;

    public TipClaimResponse(String barEmail)
    {
        this.barEmail = barEmail;
    }
}
