package edu.help.microservice.controller;

import edu.help.microservice.dto.OrderDTO;
import edu.help.microservice.dto.TipClaimRequest;
import edu.help.microservice.dto.TipClaimResponse;
import edu.help.microservice.entity.Order;
import edu.help.microservice.entity.SignUp;
import edu.help.microservice.service.BarService;
import edu.help.microservice.service.OrderService;
import edu.help.microservice.service.SignUpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final SignUpService signUpService;

    @Autowired
    public OrderController(OrderService orderService, SignUpService signUpService) {
        this.orderService = orderService;
        this.signUpService = signUpService;
    }

    // Endpoint to save an order
    @PostMapping("/save")
    public ResponseEntity<String> saveOrder(@RequestBody OrderDTO order) {
        orderService.saveOrder(order);
        return ResponseEntity.ok("Order saved successfully");
    }

    @PostMapping("/claim")
    public ResponseEntity<TipClaimResponse> claimTips(@RequestBody TipClaimRequest request) {
        try {
            // Fetch unclaimed orders
            List<Order> tipsList = orderService.getUnclaimedTips(request.getBarId(), request.getStation());
            String barEmail = signUpService.findEmailByBarId(request.getBarId());

            if (tipsList.isEmpty()) {
                return ResponseEntity.badRequest().body(new TipClaimResponse("No unclaimed tips found for your station."));
            }

            // Update orders to set tipsClaimed to bartender's name
            orderService.claimTipsForOrders(tipsList, request.getBartenderName());
            // Return success response
            return ResponseEntity.ok(new TipClaimResponse(barEmail, tipsList));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(new TipClaimResponse("Tip claim processing failed."));
        }
    }
}
