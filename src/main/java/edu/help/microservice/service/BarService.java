package edu.help.microservice.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    @Autowired // Inject PointService dependency
    private PointService pointService;

    public List<BarDTO> findAllBars() {
        List<Bar> bars = barRepository.findAll();
        List<BarDTO> barDTOs = bars.stream().map(DTOConverter::convertToBarDTO).collect(Collectors.toList());
        return barDTOs;
    }

    public Bar findByBarEmail(String bar_email) {
        return barRepository.findByBarEmail(bar_email).orElse(null);

    }

    public Bar findBarById(Integer id) {
        Optional<Bar> bar = barRepository.findById(id);
        return bar.orElse(null); // Returns the Bar if found, otherwise returns null
    }

    public List<Drink> getDrinksByBarId(Integer barId) {
        return drinkRepository.findAllDrinksByBarIdExcludingFields(barId);
    }

    public void delete(Bar bar) {
        barRepository.delete(bar);
    }

    public Bar save(Bar bar) {
        return barRepository.save(bar);
    }

    public OrderResponse processOrder(
            int barId,
            List<OrderRequest.DrinkOrder> drinkOrders,
            boolean isHappyHour,
            boolean points,
            int userId) {

        double totalPrice = 0;
        int totalQuantity = 0; // Track total quantity
        List<DrinkOrder> finalDrinkOrders = new ArrayList<>();

        // Calculate the total price or points required
        for (OrderRequest.DrinkOrder drinkOrder : drinkOrders) {
            Drink drink = drinkRepository.findById(drinkOrder.getDrinkId())
                    .orElseThrow(() -> new RuntimeException("Drink not found"));

            double price = points ? drink.getPoint().doubleValue()
                    : (isHappyHour ? drink.getDrinkDiscount().doubleValue() : drink.getDrinkPrice());

            totalPrice += price * drinkOrder.getQuantity();
            totalQuantity += drinkOrder.getQuantity();

            System.out.println("Drink: " + drink.getDrinkName() +
                    ", Quantity: " + drinkOrder.getQuantity() +
                    ", Price per unit: " + price);

            finalDrinkOrders.add(new OrderResponse.DrinkOrder(
                    drink.getDrinkId(),
                    drink.getDrinkName(),
                    drinkOrder.getQuantity()));
        }

        if (points) {
            boolean success = pointService.charge(userId, barId, (int) totalPrice, totalQuantity);
            if (!success) {
                System.out.println("Insufficient points. Transaction canceled.");
                return new OrderResponse(
                        "Insufficient points. Would you like to proceed with regular pricing?",
                        totalPrice, // Show the calculated price
                        finalDrinkOrders,
                        "broke");
            }
        } else {
            // If points are not used, add points as a reward
            pointService.addPoints(userId, barId, totalQuantity);
        }

        System.out.println("Total price: " + totalPrice);
        System.out.println("Final Drink Orders: " + finalDrinkOrders);

        return new OrderResponse(
                "Order processed successfully",
                totalPrice,
                finalDrinkOrders,
                "success");
    }



    
    public OrderResponse refundOrder(
            int barId,
            List<OrderRequest.DrinkOrder> drinkOrders,
            boolean isHappyHour,
            boolean points,
            int userId) {

        double totalPrice = 0;
        int totalQuantity = 0; // Track total quantity
        List<OrderResponse.DrinkOrder> finalDrinkOrders = new ArrayList<>();

        // Calculate total price/points to refund
        for (OrderRequest.DrinkOrder drinkOrder : drinkOrders) {
            Drink drink = drinkRepository.findById(drinkOrder.getDrinkId())
                    .orElseThrow(() -> new RuntimeException("Drink not found"));

            double price = points ? drink.getPoint().doubleValue()
                    : (isHappyHour ? drink.getDrinkDiscount().doubleValue() : drink.getDrinkPrice());

            totalPrice += price * drinkOrder.getQuantity();
            totalQuantity += drinkOrder.getQuantity();

            finalDrinkOrders.add(new OrderResponse.DrinkOrder(
                    drink.getDrinkId(),
                    drink.getDrinkName(),
                    drinkOrder.getQuantity()));
        }

        if (points) {
            // Refund points
            pointService.refund(userId, barId, (int) totalPrice, totalQuantity);
        } else {
            // Subtract previously awarded points
            pointService.subtractPoints(userId, barId, totalQuantity);
        }

        return new OrderResponse(
                "Order refunded successfully",
                totalPrice,
                finalDrinkOrders,
                "refunded");
    }
}