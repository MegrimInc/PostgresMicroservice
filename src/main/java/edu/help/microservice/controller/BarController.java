package edu.help.microservice.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import edu.help.microservice.dto.BarDTO;
import edu.help.microservice.dto.OrderRequest;
import edu.help.microservice.dto.OrderResponse;
import edu.help.microservice.entity.Drink;
import edu.help.microservice.service.BarService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
public class BarController {
    private final BarService barService;

    @GetMapping("/bars/seeAllBars")
    public List<BarDTO> seeAllBars() {
        return barService.findAllBars();        
    }

    @GetMapping("/bars/getAllDrinksByBar/{barId}")
    public List<Drink> getAllDrinksByBar(@PathVariable Integer barId) {
        return barService.getDrinksByBarId(barId);
    }

    @PostMapping("/{barId}/processOrder")
    public ResponseEntity<OrderResponse> processOrder(@PathVariable int barId, @RequestBody OrderRequest orderRequest) {
        // Delegate processing to the service layer
        OrderResponse response = barService.processOrder(barId, orderRequest);
        // Return the processed order response
        return ResponseEntity.ok(response);
    }


}
