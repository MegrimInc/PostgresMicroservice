package edu.help.microservice.service;

import edu.help.microservice.entity.Order;
import edu.help.microservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;

@RequiredArgsConstructor
@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final PointService pointService;

    public Order saveOrder(Order order) {
        if (order.getTimestamp() == null) {
            order.setTimestamp(Instant.now()); // Set current time if timestamp is missing
        }
        pointService.rewardPointsForOrder(order);
        return orderRepository.save(order);
    }
}

