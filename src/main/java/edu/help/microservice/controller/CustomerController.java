package edu.help.microservice.controller;

import java.util.Map;
import java.util.Optional;

import com.stripe.exception.StripeException;
import edu.help.microservice.dto.PaymentIdSetRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import edu.help.microservice.entity.Customer;
import edu.help.microservice.service.CustomerService;
import edu.help.microservice.service.PointService;
import edu.help.microservice.service.StripeService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/customer")
public class CustomerController {
    private final PointService pointService;
    private final CustomerService customerService;
    private final StripeService stripeService;

    @GetMapping("/points/{userId}")
    public ResponseEntity<Map<Integer, Map<Integer, Integer>>> getPointsForUser(@PathVariable int userId) {
        Map<Integer, Map<Integer, Integer>> points = pointService.getPointsForCustomerTempForEndpoint(userId);
        return ResponseEntity.ok(points);
    }

    @GetMapping("/checkPaymentMethod/{userId}")
    public ResponseEntity<Boolean> checkPaymentMethod(@PathVariable int userId) {
        Optional<Customer> customerOpt = customerService.findById(userId);
        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            boolean hasPaymentMethod = customer.getPaymentId() != null;
            return ResponseEntity.ok(hasPaymentMethod);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(false);
        }
    }

    @GetMapping("/createSetupIntent/{userId}")
    public ResponseEntity<Map<String, String>> createSetupIntent(@PathVariable int userId) {
        try {
            Map<String, String> setupIntentData = stripeService.createSetupIntent(userId);
            return ResponseEntity.ok(setupIntentData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("error", "Failed to create SetupIntent: " + e.getMessage()));
        }
    }

    @PostMapping("/addPaymentIdToDatabase")
    public ResponseEntity<Void> addPaymentIdToDatabase(@RequestBody PaymentIdSetRequest request) throws StripeException {
        stripeService.savePaymentId(request);
        return ResponseEntity.ok().build();
    }
}