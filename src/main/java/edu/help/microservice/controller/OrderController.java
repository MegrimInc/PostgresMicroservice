package edu.help.microservice.controller;

import edu.help.microservice.dto.OrderDTO;
import edu.help.microservice.entity.Order;
import edu.help.microservice.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // Endpoint to save an order
    @PostMapping("/save")
    public ResponseEntity<String> saveOrder(@RequestBody OrderDTO order) {
        orderService.saveOrder(order);
        return ResponseEntity.ok("Order saved successfully");
    }
}
