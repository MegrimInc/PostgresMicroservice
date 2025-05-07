package edu.help.microservice.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.stripe.exception.StripeException;
import edu.help.microservice.dto.CustomerNameRequest;
import edu.help.microservice.dto.CustomerNameResponse;
import edu.help.microservice.dto.MerchantDTO;
import edu.help.microservice.dto.PaymentIdSetRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import edu.help.microservice.entity.Customer;
import edu.help.microservice.entity.Item;
import edu.help.microservice.entity.Merchant;
import edu.help.microservice.service.CustomerService;
import edu.help.microservice.service.MerchantService;
import edu.help.microservice.service.PointService;
import edu.help.microservice.service.StripeService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/postgres-test-api/customer")
public class CustomerController {
    private final PointService pointService;
    private final CustomerService customerService;
    private final StripeService stripeService;
    private final MerchantService merchantService;

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

    @GetMapping("/getNames/{userId}")
    public ResponseEntity<CustomerNameResponse> getName(@PathVariable int userId) {
        return ResponseEntity.ok(customerService.getNameData(userId));
    }

    @PostMapping("/updateNames/{userId}")
    public ResponseEntity<CustomerNameResponse> getName(@RequestBody CustomerNameRequest request,
                                                        @PathVariable int userId) {
        return ResponseEntity.ok(customerService.updateNameData(request, userId));
    }

    @PostMapping("/addPaymentIdToDatabase")
    public ResponseEntity<Void> addPaymentIdToDatabase(@RequestBody PaymentIdSetRequest request) throws StripeException {
        stripeService.savePaymentId(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/cardDetails/{userId}")
    public ResponseEntity<Map<String, String>> getCardDetails(@PathVariable int userId) {
        Optional<Customer> customerOpt = customerService.findById(userId);
        if (customerOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Customer not found"));
        }
        Customer customer = customerOpt.get();
        if (customer.getPaymentId() == null || customer.getPaymentId().isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No payment method on file"));
        }
        try {
            Map<String, String> cardDetails = stripeService.getCardDetails(customer.getPaymentId());
            return ResponseEntity.ok(cardDetails);
        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Stripe error: " + e.getMessage()));
        }
    }

     @GetMapping("/seeAllMerchants")
    public List<MerchantDTO> seeAllMerchants() {
        return merchantService.findAllMerchants();
    }

    @GetMapping("/getAllItemsByMerchant/{merchantId}")
    public List<Item> getAllItemsByMerchant(@PathVariable Integer merchantId) {
        return merchantService.getItemsByMerchantId(merchantId);
    }

    @GetMapping("/{merchantId}")
    public ResponseEntity<Merchant> getMerchantById(@PathVariable Integer merchantId) {
        Merchant merchant = merchantService.findMerchantById(merchantId); // Fetch the merchant by ID
        if (merchant == null) {
            return ResponseEntity.notFound().build(); // Return 404 if not found
        }
        return ResponseEntity.ok(merchant); // Return the merchant if found
    }

}