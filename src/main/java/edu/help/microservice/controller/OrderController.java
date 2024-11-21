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

    // Endpoint to get unclaimed orders by bar ID and station
    @GetMapping("/unclaimed/{barId}/{station}")
    public ResponseEntity<List<Order>> getUnclaimedOrdersByBarAndStation(
            @PathVariable Integer barId,
            @PathVariable Character station) {
        List<Order> orders = orderService.getUnclaimedOrdersByBarAndStation(barId, station);
        return ResponseEntity.ok(orders);
    }

    // Endpoint to claim tips for a specific station by bartender name
    @PostMapping("/claim-tips")
    public ResponseEntity<String> claimTipsForStation(
            @RequestParam Integer barId,
            @RequestParam Character station,
            @RequestParam String bartenderName) {
        orderService.claimTipsForStation(barId, station, bartenderName);
        return ResponseEntity.ok("Tips claimed for station " + station + " by bartender " + bartenderName);
    }

    // Endpoint to save an order
    @PostMapping("/save")
    public ResponseEntity<String> saveOrder(@RequestBody Order order) {
        orderService.saveOrder(order);
        return ResponseEntity.ok("Order saved successfully");
    }
}
