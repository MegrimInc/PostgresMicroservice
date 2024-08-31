package edu.help.microservice.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.help.microservice.dto.BarDTO;
import edu.help.microservice.dto.OrderRequest;
import edu.help.microservice.dto.OrderResponse;
import edu.help.microservice.dto.OrderResponse.DrinkOrder;
import edu.help.microservice.entity.Bar;
import edu.help.microservice.entity.Drink;
import edu.help.microservice.repository.BarRepository;
import edu.help.microservice.repository.DrinkRepository;
import edu.help.microservice.util.DTOConverter;


@Service
public class BarService {

    @Autowired
    private BarRepository barRepository;
   

    @Autowired
    private DrinkRepository drinkRepository;

    public List<BarDTO> findAllBars()
    {
        List<Bar> bars = barRepository.findAll();
        List<BarDTO> barDTOs = bars.stream().map(DTOConverter::convertToBarDTO).collect(Collectors.toList());
        return barDTOs;
    }

    
    public Bar findByBarEmail(String bar_email) {
        return barRepository.findByBarEmail(bar_email).orElse(null);

    }

    public List<Drink> getDrinksByBarId(Integer barId) {
        return drinkRepository.findAllDrinksByBarIdExcludingFields(barId);
    }


    //REDIS STUFF LEFT ME LOCK

    public OrderResponse processOrder(int barId, List<OrderRequest.DrinkOrder> drinkOrders, boolean isHappyHour) {
        // Calculate total price based on happy hour status from OrderRequest and prepare drinks with their names and quantities
        double totalPrice = 0;
        List<DrinkOrder> finalDrinkOrders = new ArrayList<>();
        
        for (OrderRequest.DrinkOrder drinkOrder : drinkOrders) {
            Drink drink = drinkRepository.findById(drinkOrder.getDrinkId())
                .orElseThrow(() -> new RuntimeException("Drink not found"));
            double price = isHappyHour ? drink.getDrinkDiscount().doubleValue() : drink.getDrinkPrice();
            totalPrice += price * drinkOrder.getQuantity();
            System.out.println("Drink name: " + drink.getDrinkName() + ", Quantity: " + drinkOrder.getQuantity() + ", Price per unit: " + price);
            finalDrinkOrders.add(new OrderResponse.DrinkOrder(drink.getDrinkId(), drink.getDrinkName(), drinkOrder.getQuantity()));
        }
        System.out.println("Total price calculated: " + totalPrice);
        System.out.println("Final DrinkOrders: " + finalDrinkOrders);
        return new OrderResponse("Order processed successfully", totalPrice, finalDrinkOrders, "");
    }

}
