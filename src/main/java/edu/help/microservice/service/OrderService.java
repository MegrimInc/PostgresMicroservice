package edu.help.microservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.help.microservice.dto.OrderDTO;
import edu.help.microservice.entity.Order;
import edu.help.microservice.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    @Autowired
    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public void saveOrder(OrderDTO orderDTO) {
        Order order = new Order();

        // Map fields from OrderDTO to Order entity
        order.setBarId(orderDTO.getBarId());
        order.setUserId(orderDTO.getUserId());

        // Parse timestamp from String to Instant
        try {
            if (orderDTO.getTimestamp() != null) {
                order.setTimestamp(Instant.parse(orderDTO.getTimestamp()));
            } else {
                order.setTimestamp(Instant.now());
            }
        } catch (DateTimeParseException e) {
            // Handle parsing exception
            order.setTimestamp(Instant.now());
        }

        order.setDrinks(orderDTO.getDrinks());
        order.setTotalPointPrice(orderDTO.getTotalPointPrice());
        order.setTotalRegularPrice(orderDTO.getTotalRegularPrice());
        order.setTip(orderDTO.getTip());
        order.setInAppPayments(orderDTO.isInAppPayments());
        order.setStatus(orderDTO.getStatus());
        order.setStation(orderDTO.getStation());

        // Set tipsClaimed to null
        order.setTipsClaimed(null);

        // Save the order to the database
        orderRepository.save(order);
    }
}

