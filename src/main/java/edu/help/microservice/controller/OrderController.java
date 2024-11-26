package edu.help.microservice.controller;

import edu.help.microservice.dto.OrderDTO;
import edu.help.microservice.dto.TipClaimRequest;
import edu.help.microservice.dto.TipClaimResponse;
import edu.help.microservice.entity.Order;
import edu.help.microservice.service.BarService;
import edu.help.microservice.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final BarService barService;

    @Autowired
    public OrderController(OrderService orderService, BarService barService) {
        this.orderService = orderService;
        this.barService = barService;
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

            if (tipsList.isEmpty()) {
                return ResponseEntity.badRequest().body(new TipClaimResponse("No unclaimed tips found for your station."));
            }

            // Update orders to set tipsClaimed to bartender's name
            orderService.claimTipsForOrders(tipsList, request.getBartenderName());

            // Retrieve bar email address
            String barEmail = barService.findEmailById(request.getBarId());

//            // Prepare email content
//            String emailContent = emailService.prepareEmailContent(request, tipsList);
//
//            // Send emails to bar and bartender (if email provided)
//            emailService.sendTipEmail(barEmail, "Tip Report", emailContent);
//            if (request.getBartenderEmail() != null && !request.getBartenderEmail().isEmpty()) {
//                emailService.sendTipEmail(request.getBartenderEmail(), "Tip Report", emailContent);
//            }

            // Return success response
            return ResponseEntity.ok(new TipClaimResponse("Tip Claim Successful", tipsList));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(new TipClaimResponse("Tip claim processing failed."));
        }
    }
}
