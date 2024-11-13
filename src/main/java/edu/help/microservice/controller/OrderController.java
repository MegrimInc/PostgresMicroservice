package edu.help.microservice.controller;

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

    // Endpoint to get order history for a user
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> getOrderHistoryByUserId(@PathVariable Integer userId) {
        List<Order> orders = orderService.getOrderHistoryByUserId(userId);
        return ResponseEntity.ok(orders);
    }

    // Endpoint to get unclaimed orders by station
    @GetMapping("/unclaimed/{station}")
    public ResponseEntity<List<Order>> getUnclaimedOrdersByStation(@PathVariable Character station) {
        List<Order> orders = orderService.getUnclaimedOrdersByStation(station);
        return ResponseEntity.ok(orders);
    }

    // Endpoint to claim tips for a specific station
    @PostMapping("/claim-tips/{station}")
    public ResponseEntity<String> claimTipsForStation(@PathVariable Character station) {
        orderService.claimTipsForStation(station);
        return ResponseEntity.ok("Tips claimed for station " + station);
    }
}
