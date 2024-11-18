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

    // Get unclaimed orders by bar ID and station
    public List<Order> getUnclaimedOrdersByBarAndStation(Integer barId, Character station) {
        return orderRepository.findUnclaimedOrdersByBarAndStation(barId, station);
    }

    // Mark tips as claimed by bartender name
    public void claimTipsForStation(Integer barId, Character station, String bartenderName) {
        orderRepository.markTipsAsClaimedByBartender(barId, station, bartenderName);
    }
}
