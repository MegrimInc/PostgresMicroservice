package edu.help.microservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.help.microservice.dto.OrderToSave;
import edu.help.microservice.entity.Order;
import edu.help.microservice.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
        this.objectMapper = new ObjectMapper(); // Initialize ObjectMapper
    }

    // Get unclaimed orders by bar ID and station
    public List<Order> getUnclaimedOrdersByBarAndStation(Integer barId, Character station) {
        return orderRepository.findUnclaimedOrdersByBarAndStation(barId, station);
    }

    // Mark tips as claimed by bartender name
    public void claimTipsForStation(Integer barId, Character station, String bartenderName) {
        orderRepository.markTipsAsClaimedByBartender(barId, station, bartenderName);
    }

    // Save an order to the database
    public void saveOrder(OrderToSave orderToSave) {
        try {
            Order orderEntity = new Order();

            // Set fields from OrderToSave to Order entity
            orderEntity.setBarId(orderToSave.getBarId());
            orderEntity.setUserId(orderToSave.getUserId());
            orderEntity.setTimestamp(Timestamp.valueOf(orderToSave.getTimestamp())); // Convert to SQL timestamp
            orderEntity.setPointPrice(orderToSave.getPointPrice());
            orderEntity.setDollarPrice(orderToSave.getDollarPrice());
            orderEntity.setTipAmount(orderToSave.getTipAmount());
            orderEntity.setStatus(orderToSave.getStatus());
            orderEntity.setStation(orderToSave.getClaimer());
            orderEntity.setTipsClaimed(null);

            // Serialize drinks to JSON and set it in the entity
            String drinksJson = objectMapper.writeValueAsString(orderToSave.getDrinkIds());
            orderEntity.setDrinkIds(drinksJson);

            // Save the order to the database
            orderRepository.save(orderEntity);
        } catch (JsonProcessingException e) {
            // Handle JSON serialization exceptions
            System.err.println("Error serializing drinks to JSON: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            // Handle other exceptions
            System.err.println("Error saving order: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
