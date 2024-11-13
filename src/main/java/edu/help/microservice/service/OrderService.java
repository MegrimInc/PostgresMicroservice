package edu.help.microservice.service;

import edu.help.microservice.entity.Order;
import edu.help.microservice.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    @Autowired
    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // Get order history for a specific user
    public List<Order> getOrderHistoryByUserId(Integer userId) {
        return orderRepository.findByUserId(userId);
    }

    // Get unclaimed orders by station
    public List<Order> getUnclaimedOrdersByStation(Character station) {
        return orderRepository.findUnclaimedOrdersByStation(station);
    }

    // Mark tips as claimed for a specific station
    public void claimTipsForStation(Character station) {
        orderRepository.markTipsAsClaimedForStation(station);
    }
}
