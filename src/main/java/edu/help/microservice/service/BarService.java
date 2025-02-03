package edu.help.microservice.service;

import java.time.LocalDate;
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

    /**
     * Returns all Bars in the database as BarDTO objects.
     */
    public List<BarDTO> findAllBars() {
        List<Bar> bars = barRepository.findAll();
        return bars.stream()
                .map(DTOConverter::convertToBarDTO)
                .collect(Collectors.toList());
    }

    /**
     * Finds a Bar by its email.
     * @param bar_email The email of the bar.
     * @return The Bar if found, otherwise null.
     */
    public Bar findByBarEmail(String bar_email) {
        return barRepository.findByBarEmail(bar_email).orElse(null);
    }

    /**
     * Finds a Bar by its ID.
     * @param id The bar's ID.
     * @return The Bar if found, otherwise null.
     */
    public Bar findBarById(Integer id) {
        Optional<Bar> bar = barRepository.findById(id);
        return bar.orElse(null);
    }

    /**
     * Retrieves all drinks for a given Bar ID (excluding certain fields).
     */
    public List<Drink> getDrinksByBarId(Integer barId) {
        return drinkRepository.findAllDrinksByBarIdExcludingFields(barId);
    }

    /**
     * Deletes a Bar from the database.
     */
    public void delete(Bar bar) {
        barRepository.delete(bar);
    }

    /**
     * Saves (creates or updates) a Bar object.
     * @param bar The Bar to save.
     * @return The saved Bar object.
     */
    public Bar save(Bar bar) {
        return barRepository.save(bar);
    }


    /**
     * Sets the startDate column for a given bar, using our custom native query.
     *
     * @param barId The ID of the bar.
     * @param date The LocalDate to set (works best if the column is a DATE type).
     */
    public void setStartDate(Integer barId, LocalDate date) {
        barRepository.updateStartDate(barId, date);
    }

    public LocalDate getStartDate(Integer barId) {
        Bar bar = findBarById(barId);
        return (bar != null) ? bar.getStartDate() : null;
    }

    /**
     * Processes an order (e.g. purchase or points usage) for a given Bar.
     */
    public OrderResponse processOrder(int barId, OrderRequest request) {
        double totalMoneyPrice = 0;
        int totalPointsPrice = 0;
        int totalDrinkQuantity = 0;

        // Retrieve user's name from CustomerService
        String userName = customerService.getName(request.getUserId());

        List<DrinkOrderResponse> drinkOrderResponses = new ArrayList<>();

        // Process each DrinkOrderRequest
        for (DrinkOrderRequest drinkOrderRequest : request.getDrinks()) {
            Drink drink = drinkRepository.findById(drinkOrderRequest.getDrinkId())
                    .orElseThrow(() -> new RuntimeException("Drink not found"));

            String sizeType = drinkOrderRequest.getSizeType();
            if (sizeType == null || sizeType.isEmpty()) {
                sizeType = "";  // Treat null or empty as no size specified
            }

            // Build the response for each drink
            drinkOrderResponses.add(
                    DrinkOrderResponse.builder()
                            .drinkId(drink.getDrinkId())
                            .drinkName(drink.getDrinkName())
                            .paymentType(drinkOrderRequest.getPaymentType())
                            .sizeType(sizeType)
                            .quantity(drinkOrderRequest.getQuantity())
                            .build()
            );

            // Count total drinks
            totalDrinkQuantity += drinkOrderRequest.getQuantity();

            // If user pays with points
            if ("points".equalsIgnoreCase(drinkOrderRequest.getPaymentType())) {
                // Price in points
                totalPointsPrice += drink.getPoint() * drinkOrderRequest.getQuantity();
                continue;
            }

            // If user pays with money, calculate price
            double price;
            if (request.isHappyHour()) {
                // Use the happy hour price
                if ("double".equals(sizeType)) {
                    price = drink.getDoubleHappyPrice();
                } else {
                    price = drink.getSingleHappyPrice();
                }
            } else {
                // Use the regular price
                if ("double".equals(sizeType)) {
                    price = drink.getDoublePrice();
                } else {
                    price = drink.getSinglePrice();
                }
            }

            totalMoneyPrice += price * drinkOrderRequest.getQuantity();
        }

        // Calculate tip, if any
        double tipAmount = Math.round(request.getTip() * totalMoneyPrice * 100) / 100.0;

        // Check if user has enough points to cover the point-based portion
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

        // If using in-app payments, attempt to process via Stripe
        if (request.isInAppPayments()) {
            try {
                stripeService.processOrder(totalMoneyPrice, tipAmount, request.getUserId(), barId);
            } catch (StripeException exception) {
                // Log Stripe exception
                System.out.println("Stripe error: " + exception.getMessage());
                return OrderResponse.builder()
                        .message("Stripe error")
                        .messageType("error")
                        .tip(tipAmount)
                        .totalPrice(totalMoneyPrice)
                        .totalPointPrice(totalPointsPrice)
                        .drinks(drinkOrderResponses)
                        .name(userName)
                        .build();
            } catch (InvalidStripeChargeException exception) {
                System.out.println(exception.getMessage());
                return OrderResponse.builder()
                        .message("Customer payment error")
                        .messageType("error")
                        .tip(tipAmount)
                        .totalPrice(totalMoneyPrice)
                        .totalPointPrice(totalPointsPrice)
                        .drinks(drinkOrderResponses)
                        .name(userName)
                        .build();
            }
        }

        // Deduct the points
        pointService.chargeCustomer(totalPointsPrice, request.getUserId(), barId);

        // Return a successful response
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
