package edu.help.microservice.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.stripe.exception.StripeException;
import edu.help.microservice.dto.CustomerNameRequest;
import edu.help.microservice.dto.CustomerNameResponse;
import edu.help.microservice.dto.InventoryResponse;
import edu.help.microservice.dto.MerchantDTO;
import edu.help.microservice.dto.PaymentIdSetRequest;
import edu.help.microservice.entity.Message;
import edu.help.microservice.repository.MessageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import edu.help.microservice.entity.Customer;
import edu.help.microservice.entity.Merchant;
import edu.help.microservice.service.CustomerService;
import edu.help.microservice.service.MerchantService;
import edu.help.microservice.service.PointService;
import edu.help.microservice.service.StripeService;
import lombok.RequiredArgsConstructor;
import static edu.help.microservice.config.ApiConfig.BASE_PATH;

@RequiredArgsConstructor
@RestController
@RequestMapping(BASE_PATH + "/customer")
public class CustomerController {
    private final PointService pointService;
    private final CustomerService customerService;
    private final StripeService stripeService;
    private final MerchantService merchantService;
    private final MessageRepository messageRepository;


    @PostMapping("/sendMessage")
    public ResponseEntity<Message> sendMessage(@RequestBody Message message) {
        return ResponseEntity.ok(messageRepository.save(message));
    }

    @GetMapping("/conversation")
    public ResponseEntity<List<Message>> getConversation(
            @RequestParam Integer customerId,
            @RequestParam Integer merchantId) {
        List<Message> messages = messageRepository.findByCustomerIdAndMerchantIdOrderByCreatedAt(customerId, merchantId);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/points/{customerId}")
    public ResponseEntity<Map<Integer, Map<Integer, Integer>>> getPointsForCustomer(@PathVariable int customerId) {
        Map<Integer, Map<Integer, Integer>> points = pointService.getPointsForCustomerTempForEndpoint(customerId);
        return ResponseEntity.ok(points);
    }

    @GetMapping("/checkPaymentMethod/{customerId}")
    public ResponseEntity<Boolean> checkPaymentMethod(@PathVariable int customerId) {
        Optional<Customer> customerOpt = customerService.findById(customerId);
        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            boolean hasPaymentMethod = customer.getPaymentId() != null;
            return ResponseEntity.ok(hasPaymentMethod);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(false);
        }
    }

    @GetMapping("/createSetupIntent/{customerId}")
    public ResponseEntity<Map<String, String>> createSetupIntent(@PathVariable int customerId) {
        try {
            Map<String, String> setupIntentData = stripeService.createSetupIntent(customerId);
            return ResponseEntity.ok(setupIntentData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create SetupIntent: " + e.getMessage()));
        }
    }

    @GetMapping("/getNames/{customerId}")
    public ResponseEntity<CustomerNameResponse> getName(@PathVariable int customerId) {
        return ResponseEntity.ok(customerService.getNameData(customerId));
    }

    @PostMapping("/updateNames/{customerId}")
    public ResponseEntity<CustomerNameResponse> getName(@RequestBody CustomerNameRequest request,
            @PathVariable int customerId) {
        return ResponseEntity.ok(customerService.updateNameData(request, customerId));
    }

    @PostMapping("/addPaymentIdToDatabase")
    public ResponseEntity<Void> addPaymentIdToDatabase(@RequestBody PaymentIdSetRequest request)
            throws StripeException {
        stripeService.savePaymentId(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/cardDetails/{customerId}")
    public ResponseEntity<Map<String, String>> getCardDetails(@PathVariable int customerId) {
        Optional<Customer> customerOpt = customerService.findById(customerId);
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

    @GetMapping("/isMerchantOpen/{merchantId}")
    public ResponseEntity<Boolean> isMerchantOpen(@PathVariable int merchantId) {
        Merchant merchant = merchantService.findMerchantById(merchantId);
        if (merchant == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(false);
        }
        Boolean isOpen = merchant.getIsOpen();
        return ResponseEntity.ok(isOpen != null && isOpen);
    }

    @GetMapping("/getInventoryByMerchant/{merchantId}")
    public InventoryResponse getInventoryByMerchant(@PathVariable Integer merchantId) {
        return merchantService.getInventoryByMerchantId(merchantId);
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