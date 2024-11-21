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

    @Autowired
    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<Order> getUnclaimedOrdersByBarAndStation(Integer barId, Character station) {
        return orderRepository.findUnclaimedOrdersByBarAndStation(barId, station);
    }

    public void claimTipsForStation(Integer barId, Character station, String bartenderName) {
        orderRepository.markTipsAsClaimedByBartender(barId, station, bartenderName);
    }

    public Order saveOrder(Order order) {
        if (order.getTimestamp() == null) {
            order.setTimestamp(Instant.now()); // Set current time if timestamp is missing
        }
        return orderRepository.save(order);
    }
}

