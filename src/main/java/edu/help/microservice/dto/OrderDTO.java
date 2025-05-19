package edu.help.microservice.dto;

import edu.help.microservice.entity.Order.ItemOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private int merchantId;
    private int customerId;
    private String timestamp; // Note it is a string.
    private List<ItemOrder> items;
    private int totalPointPrice;
    private double totalRegularPrice;
    private double totalGratuity;
    private double totalServiceFee;
    private double totalTax;
    private boolean inAppPayments;
    private String status;
    private String terminal;
    private boolean pointOfSale;
    // Exclude fields not present in the Order entity
}