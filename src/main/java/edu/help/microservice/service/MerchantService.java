package edu.help.microservice.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.stripe.exception.StripeException;

import edu.help.microservice.dto.MerchantDto;
import edu.help.microservice.dto.ItemOrderRequest;
import edu.help.microservice.dto.ItemOrderResponse;
import edu.help.microservice.dto.OrderRequest;
import edu.help.microservice.dto.OrderResponse;
import edu.help.microservice.entity.Merchant;
import edu.help.microservice.entity.Item;
import edu.help.microservice.exception.InvalidStripeChargeException;
import edu.help.microservice.repository.MerchantRepository;
import edu.help.microservice.repository.ItemRepository;
import edu.help.microservice.util.DTOConverter;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final ItemRepository itemRepository;
    private final PointService pointService;
    private final StripeService stripeService;
    private final CustomerService customerService;

    /**
     * Returns all Merchants in the database as MerchantDTO objects.
     */
    public List<MerchantDto> findAllMerchants() {
        List<Merchant> merchants = merchantRepository.findAll();
        return merchants.stream()
                .map(DTOConverter::convertToMerchantDTO)
                .collect(Collectors.toList());
    }

    /**
     * Finds a Merchant by its email.
     * @param merchant_email The email of the merchant.
     * @return The Merchant if found, otherwise null.
     */
    public Merchant findByMerchantEmail(String merchant_email) {
        return merchantRepository.findByMerchantEmail(merchant_email).orElse(null);
    }

    /**
     * Finds a Merchant by its ID.
     * @param id The merchant's ID.
     * @return The Merchant if found, otherwise null.
     */
    public Merchant findMerchantById(Integer id) {
        Optional<Merchant> merchant = merchantRepository.findById(id);
        return merchant.orElse(null);
    }

    /**
     * Retrieves all items for a given Merchant ID (excluding certain fields).
     */
    public List<Item> getItemsByMerchantId(Integer merchantId) {
        return itemRepository.findAllItemsByMerchantIdExcludingFields(merchantId);
    }

    /**
     * Deletes a Merchant from the database.
     */
    public void delete(Merchant merchant) {
        merchantRepository.delete(merchant);
    }

    /**
     * Saves (creates or updates) a Merchant object.
     * @param merchant The Merchant to save.
     * @return The saved Merchant object.
     */
    public Merchant save(Merchant merchant) {
        return merchantRepository.save(merchant);
    }

    /**
     * Sets the startDate column for a given merchant, using our custom native query.
     *
     * @param merchantId The ID of the merchant.
     * @param date The LocalDate to set (works best if the column is a DATE type).
     */
    public void setStartDate(Integer merchantId, LocalDate date) {
        merchantRepository.updateStartDate(merchantId, date);
    }

    public LocalDate getStartDate(Integer merchantId) {
        Merchant merchant = findMerchantById(merchantId);
        return (merchant != null) ? merchant.getStartDate() : null;
    }

    /**
     * Processes an order (e.g. purchase or points usage) for a given Merchant.
     */
    public OrderResponse processOrder(int merchantId, OrderRequest request) {
        double totalMoneyPrice = 0;
        int totalPointsPrice = 0;
        int totalItemQuantity = 0;

        // Retrieve user's name from CustomerService
        String userName = customerService.getName(request.getUserId());

        List<ItemOrderResponse> itemOrderResponses = new ArrayList<>();

        // Process each ItemOrderRequest
        for (ItemOrderRequest itemOrderRequest : request.getItems()) {
            Item item = itemRepository.findById(itemOrderRequest.getItemId())
                    .orElseThrow(() -> new RuntimeException("Item not found"));

            String sizeType = itemOrderRequest.getSizeType();
            if (sizeType == null || sizeType.isEmpty()) {
                sizeType = ""; // Treat null or empty as no size specified
            }

            // Build the response for each item
            itemOrderResponses.add(
                    ItemOrderResponse.builder()
                            .itemId(item.getItemId())
                            .itemName(item.getItemName())
                            .paymentType(itemOrderRequest.getPaymentType())
                            .sizeType(sizeType)
                            .quantity(itemOrderRequest.getQuantity())
                            .build());

            // Count total items
            totalItemQuantity += itemOrderRequest.getQuantity();

            // If user pays with points
            if ("points".equalsIgnoreCase(itemOrderRequest.getPaymentType())) {
                // Price in points
                totalPointsPrice += item.getPoint() * itemOrderRequest.getQuantity();
                continue;
            }

            // If user pays with money, calculate price
            double price;
            if (request.isHappyHour()) {
                // Use the happy hour price
                if ("double".equals(sizeType)) {
                    price = item.getDoubleHappyPrice();
                } else {
                    price = item.getSingleHappyPrice();
                }
            } else {
                // Use the regular price
                if ("double".equals(sizeType)) {
                    price = item.getDoublePrice();
                } else {
                    price = item.getSinglePrice();
                }
            }

            totalMoneyPrice += price * itemOrderRequest.getQuantity();
        }

        // Calculate tip, if any
        double tipAmount = Math.round(request.getTip() * totalMoneyPrice * 100) / 100.0;

        // Check if user has enough points to cover the point-based portion
        if (!pointService.customerHasRequiredBalance(totalPointsPrice, request.getUserId(), merchantId)) {
            return OrderResponse.builder()
                    .message("Insufficient points.")
                    .messageType("error")
                    .tip(tipAmount)
                    .totalPrice(totalMoneyPrice)
                    .totalPointPrice(totalPointsPrice)
                    .items(itemOrderResponses)
                    .name(userName)
                    .build();
        }

        // If using in-app payments, attempt to process via Stripe
        if (request.isInAppPayments()) {
            try {
                stripeService.processOrder(totalMoneyPrice, tipAmount, request.getUserId(), merchantId);
            } catch (StripeException exception) {
                // Log Stripe exception
                System.out.println("Stripe error: " + exception.getMessage());
                return OrderResponse.builder()
                        .message(
                                "There was an issue processing your payment. Please check your card details or try another payment method.")
                        .messageType("error")
                        .tip(tipAmount)
                        .totalPrice(totalMoneyPrice)
                        .totalPointPrice(totalPointsPrice)
                        .items(itemOrderResponses)
                        .name(userName)
                        .build();
            } catch (InvalidStripeChargeException exception) {
                System.out.println(exception.getMessage());
                return OrderResponse.builder()
                        .message("There was an issue processing your payment. Please check your card details or try another payment method.")
                        .messageType("error")
                        .tip(tipAmount)
                        .totalPrice(totalMoneyPrice)
                        .totalPointPrice(totalPointsPrice)
                        .items(itemOrderResponses)
                        .name(userName)
                        .build();
            }
        }

        // Deduct the points
        pointService.chargeCustomer(totalPointsPrice, request.getUserId(), merchantId);

        // Return a successful response
        return OrderResponse.builder()
                .message("Order processed successfully")
                .messageType("success")
                .tip(tipAmount)
                .totalPrice(totalMoneyPrice)
                .totalPointPrice(totalPointsPrice)
                .items(itemOrderResponses)
                .name(userName)
                .build();
    }
}