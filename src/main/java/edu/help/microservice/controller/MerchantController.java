package edu.help.microservice.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import edu.help.microservice.dto.MerchantDto;
import edu.help.microservice.dto.OrderRequest;
import edu.help.microservice.dto.OrderResponse;
import edu.help.microservice.entity.Merchant;
import edu.help.microservice.entity.Item;
import edu.help.microservice.service.MerchantService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
public class MerchantController {
    private final MerchantService merchantService;

    @GetMapping("/merchants/seeAllMerchants")
    public List<MerchantDto> seeAllMerchants() {
        return merchantService.findAllMerchants();
    }

    @GetMapping("/merchants/getAllItemsByMerchant/{merchantId}")
    public List<Item> getAllItemsByMerchant(@PathVariable Integer merchantId) {
        return merchantService.getItemsByMerchantId(merchantId);
    }

    @PostMapping("/{merchantId}/processOrder")
    public ResponseEntity<OrderResponse> processOrder(@PathVariable int merchantId, @RequestBody OrderRequest orderRequest) {
        // Delegate processing to the service layer
        OrderResponse response = merchantService.processOrder(merchantId, orderRequest);
        // Return the processed order response
        return ResponseEntity.ok(response);
    }

    @GetMapping("/merchants/{merchantId}")
    public ResponseEntity<Merchant> getMerchantById(@PathVariable Integer merchantId) {
        Merchant merchant = merchantService.findMerchantById(merchantId); // Fetch the merchant by ID
        if (merchant == null) {
            return ResponseEntity.notFound().build(); // Return 404 if not found
        }
        return ResponseEntity.ok(merchant); // Return the merchant if found
    }

}