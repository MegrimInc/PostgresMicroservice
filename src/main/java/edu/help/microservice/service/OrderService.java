package edu.help.microservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.help.microservice.entity.Order;
import edu.help.microservice.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
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

    // Save an order
    public Order saveOrder(Order order) {
        if (order.getTimestamp() == null) {
            order.setTimestamp(Instant.now()); // Ensure timestamp is set to current time if not provided
        }

        // Save the order using the repository
        return orderRepository.save(order);
    }
}
