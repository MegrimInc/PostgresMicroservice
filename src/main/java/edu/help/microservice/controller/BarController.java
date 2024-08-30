package edu.help.microservice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.help.microservice.dto.BarDTO;
import edu.help.microservice.dto.OrderRequest;
import edu.help.microservice.dto.OrderResponse;
import edu.help.microservice.entity.Drink;
import edu.help.microservice.service.BarService;

@RestController
public class BarController {

    @Autowired
    private BarService barService;

    @GetMapping("/bars/seeAllBars")
    public List<BarDTO> seeAllBars()
    {
        return barService.findAllBars();        
    }

    // New endpoint to get 6 random drinks for a specific category and bar
    @GetMapping("/bars/getRandomDrinks")
    public List<Drink> getRandomDrinks(@RequestParam Integer categoryId, @RequestParam Integer barId) {
        return barService.findRandom6DrinksByCategoryIdAndBarId(categoryId, barId);
    }

    @PostMapping("/{barId}/processOrder")
    public OrderResponse processOrder(@PathVariable int barId, @RequestBody OrderRequest orderRequest) {
        return barService.processOrder(barId, orderRequest.getDrinks(), orderRequest.isHappyHour());

        //processOrder(orderRequest, boolean)
    }
    

}
