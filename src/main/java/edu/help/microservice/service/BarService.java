package edu.help.microservice.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.stripe.exception.StripeException;

import edu.help.microservice.dto.BarDTO;
import edu.help.microservice.dto.DrinkOrderRequest;
import edu.help.microservice.dto.DrinkOrderResponse;
import edu.help.microservice.dto.OrderRequest;
import edu.help.microservice.dto.OrderResponse;
import edu.help.microservice.entity.Bar;
import edu.help.microservice.entity.Drink;
import edu.help.microservice.repository.BarRepository;
import edu.help.microservice.repository.DrinkRepository;
import edu.help.microservice.util.DTOConverter;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class BarService {
    private final BarRepository barRepository;
    private final DrinkRepository drinkRepository;
    private final PointService pointService;
    private final StripeService stripeService;
    private final CustomerService customerService;

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

    public OrderResponse processOrder(int barId, OrderRequest request) {
        double totalMoneyPrice = 0;
        int totalPointsPrice = 0;
        int totalDrinkQuantity = 0;
        String userName = customerService.getName(request.getUserId());
        List<DrinkOrderResponse> drinkOrderResponses = new ArrayList<>();

        // Process drinks
        for (DrinkOrderRequest drinkOrderRequest : request.getDrinks()) {
            Drink drink = drinkRepository.findById(drinkOrderRequest.getDrinkId())
                    .orElseThrow(() -> new RuntimeException("Drink not found"));

            String sizeType = drinkOrderRequest.getSizeType();

            // Handle null or empty sizeType
            if (sizeType == null || sizeType.isEmpty()) {
                sizeType = ""; // Treat null as empty string
            }

            drinkOrderResponses.add(DrinkOrderResponse.builder()
                    .drinkId(drink.getDrinkId())
                    .drinkName(drink.getDrinkName())
                    .paymentType(drinkOrderRequest.getPaymentType())
                    .sizeType(sizeType)
                    .quantity(drinkOrderRequest.getQuantity())
                    .build());

            totalDrinkQuantity += drinkOrderRequest.getQuantity();

            if ("points".equals(drinkOrderRequest.getPaymentType())) {
                totalPointsPrice += drink.getPoint() * drinkOrderRequest.getQuantity();
                continue;
            }

            double price;
            if (request.isHappyHour()) {
                if ("double".equals(sizeType)) {
                    price = drink.getDoubleHappyPrice();
                } else {
                    price = drink.getSingleHappyPrice();
                }
            } else {
                if ("double".equals(sizeType)) {
                    price = drink.getDoublePrice();
                } else {
                    price = drink.getSinglePrice();
                }
            }

            totalMoneyPrice += price * drinkOrderRequest.getQuantity();
        }

        double tipAmount = (double) Math.round(request.getTip() * totalMoneyPrice * 100) / 100;

        if (!pointService.customerHasRequiredBalance(totalPointsPrice, request.getUserId(), barId)) {
            return OrderResponse.builder()
                    .message("Insufficient points.")
                    .messageType("error")
                    .tip(tipAmount)
                    .totalPrice(totalMoneyPrice)
                    .totalPointPrice(totalPointsPrice)
                    .drinks(drinkOrderResponses)
                    .name(userName)
                    .build();
        }

        // Process payment
        if (request.isInAppPayments()) {
            try {
                stripeService.processOrder(totalMoneyPrice, tipAmount, request.getUserId(), barId);
            } catch (StripeException exception) {
                System.out.println(exception.getMessage());
                return OrderResponse.builder()
                        .message("Stripe error")
                        .messageType("error")
                        .tip(tipAmount)
                        .totalPrice(totalMoneyPrice)
                        .totalPointPrice(totalPointsPrice)
                        .drinks(drinkOrderResponses)
                        .name(userName)
                        .build();
            }
        }

        // Reward user with points / charge them for used points
        pointService.chargeCustomer(totalPointsPrice, request.getUserId(), barId);

        return OrderResponse.builder()
                .message("Order processed successfully")
                .messageType("success")
                .tip(tipAmount)
                .totalPrice(totalMoneyPrice)
                .totalPointPrice(totalPointsPrice)
                .drinks(drinkOrderResponses)
                .name(userName)
                .build();
    }
}
