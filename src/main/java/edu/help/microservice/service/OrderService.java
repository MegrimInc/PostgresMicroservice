package edu.help.microservice.service;

import edu.help.microservice.dto.ItemCountDTO;
import edu.help.microservice.dto.ItemOrderRequest;
import edu.help.microservice.dto.ItemOrderResponse;
import edu.help.microservice.entity.Item;
import edu.help.microservice.entity.Config;
import edu.help.microservice.repository.ItemRepository;
import edu.help.microservice.repository.ConfigRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import edu.help.microservice.dto.OrderDTO;
import edu.help.microservice.dto.OrderRequest;
import edu.help.microservice.dto.OrderResponse;
import edu.help.microservice.entity.Order;
import edu.help.microservice.exception.InvalidStripeChargeException;
import edu.help.microservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import com.stripe.exception.StripeException;

@RequiredArgsConstructor
@Service
public class OrderService {

    private final ItemRepository itemRepository;
    private final OrderRepository orderRepository;
    private final PointService pointService;
    private final StripeService stripeService;
    private final CustomerService customerService;
    private final ConfigRepository configRepository;

    public void saveOrder(OrderDTO orderDTO) {
        Order order = new Order();

        // Map fields from OrderDTO to Order entity
        order.setMerchantId(orderDTO.getMerchantId());
        order.setCustomerId(orderDTO.getCustomerId());

        // Parse timestamp from String to Instant
        try {
            if (orderDTO.getTimestamp() != null) {
                order.setTimestamp(Instant.parse(orderDTO.getTimestamp()));
            } else {
                order.setTimestamp(Instant.now());
            }
        } catch (DateTimeParseException e) {
            // Handle parsing exception
            order.setTimestamp(Instant.now());
        }

        order.setItems(orderDTO.getItems());
        order.setTotalPointPrice(orderDTO.getTotalPointPrice());
        order.setTotalRegularPrice(orderDTO.getTotalRegularPrice());
        order.setTotalGratuity(orderDTO.getTotalGratuity());
        order.setTotalServiceFee(orderDTO.getTotalServiceFee());
        order.setTotalTax(orderDTO.getTotalTax());
        order.setInAppPayments(orderDTO.isInAppPayments());
        order.setStatus(orderDTO.getStatus());
        order.setTerminal(orderDTO.getTerminal());
        order.setPointOfSale(orderDTO.isPointOfSale());
        order.setClaimer(null);
        orderRepository.save(order);
        
    }

    @Transactional(readOnly = true)
    public List<Order> getUnclaimedTips(int merchantId, String station) {
        return orderRepository.findUnclaimedTipsByMerchantIdAndTerminal(merchantId, station);
    }

    @Transactional
    public void claimTipsForOrders(List<Order> orders, String claimer) {
        List<Integer> orderIds = orders.stream().map(Order::getOrderId).toList();
        orderRepository.updateClaimer(claimer, orderIds);
    }

    /**
     * Retrieves all orders for a given merchant.
     */
    @Transactional(readOnly = true)
    public List<Order> getAllOrdersForMerchant(int merchantId) {
        return orderRepository.findByMerchantId(merchantId);
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByDateRange(int merchantId, Instant start, Instant end) {
        return orderRepository.findByMerchantIdAndTimestampBetween(merchantId, start, end);
    }

    @Transactional(readOnly = true)
    public List<Order> getByDay(int merchantId, Date day) {
        return orderRepository.findByMerchantIdAndDate(merchantId, day);
    }

    @Transactional(readOnly = true)
    public List<Order> getFiftyOrders(int merchantId, Instant startingInstant, int index) {
        Pageable pageable = PageRequest.of(index, 50, Sort.by("timestamp").descending());
        Page<Order> page = orderRepository.findByMerchantIdAndTimestampLessThanEqualOrderByTimestampDesc(merchantId,
                startingInstant, pageable);
        return page.getContent();
    }

    // New method: Get top 5 most ordered items for a merchant
    @Transactional(readOnly = true)
    public Map<String, Integer> getTop5Items(int merchantId) {
        List<Object[]> results = orderRepository.findTop5ItemsByMerchantId(merchantId);
        Map<String, Integer> top5Items = new HashMap<>();
        for (Object[] row : results) {
            String itemName = (String) row[0];
            Integer totalQuantity = ((Number) row[1]).intValue();
            top5Items.put(itemName, totalQuantity);
        }
        return top5Items;
    }

    @Transactional(readOnly = true)
    public List<ItemCountDTO> getAllItemCountsForMerchant(int merchantId) {
        // 1. Fetch all items for the merchant
        List<Item> items = itemRepository.findByMerchantId(merchantId);

        // 2. Fetch all orders for the merchant
        List<Order> orders = orderRepository.findByMerchantId(merchantId);

        // 3. Map: itemId → [dollarCount, pointCount]
        Map<Integer, int[]> itemStats = new HashMap<>();

        for (Order order : orders) {
            for (Order.ItemOrder itemOrder : order.getItems()) {
                int id = itemOrder.getItemId();
                int quantity = itemOrder.getQuantity();
                String payment = itemOrder.getPaymentType();

                int[] stats = itemStats.getOrDefault(id, new int[] { 0, 0 });
                if ("points".equalsIgnoreCase(payment)) {
                    stats[1] += quantity;
                } else {
                    stats[0] += quantity; // default to regular/dollars
                }
                itemStats.put(id, stats);
            }
        }

        // 4. Build DTO list from items + stats
        List<ItemCountDTO> result = new ArrayList<>();
        for (Item item : items) {
            int itemId = item.getItemId();
            String name = item.getName();
            double regularPrice = item.getRegularPrice();

            int[] stats = itemStats.getOrDefault(itemId, new int[] { 0, 0 });
            int soldWithDollars = stats[0];
            int soldWithPoints = stats[1];
            int totalSold = soldWithDollars + soldWithPoints;

            result.add(new ItemCountDTO(itemId, name, regularPrice, soldWithDollars, soldWithPoints, totalSold));
        }

        // Sort by totalSold DESC
        result.sort((a, b) -> Integer.compare(b.getTotalSold(), a.getTotalSold()));
        return result;

    }

    /**
     * Processes an order (e.g. purchase or points usage) for a given Merchant.
     */
    public OrderResponse processOrder(int merchantId, OrderRequest request) {
        double totalMoneyPrice = 0;
        int totalPointsPrice = 0;
        double totalTax = 0;
        double totalGratuity = 0;
        double totalServiceFee = 0;
        double finalTotal = 0;

        // Retrieve customer's name from CustomerService
        String customerName = customerService.getName(request.getCustomerId());

        List<ItemOrderResponse> itemOrderResponses = new ArrayList<>();

        // Process each ItemOrderRequest
        for (ItemOrderRequest itemOrderRequest : request.getItems()) {
            Item item = itemRepository.findById(itemOrderRequest.getItemId())
                    .orElseThrow(() -> new RuntimeException("Item not found"));

            // Build the response for each item
            itemOrderResponses.add(
                    ItemOrderResponse.builder()
                            .itemId(item.getItemId())
                            .itemName(item.getName())
                            .paymentType(itemOrderRequest.getPaymentType())
                            .quantity(itemOrderRequest.getQuantity())
                            .build());

            int quantity = itemOrderRequest.getQuantity();

            // If customer pays with points
            if ("points".equalsIgnoreCase(itemOrderRequest.getPaymentType())) {
                // Price in points
                totalPointsPrice += item.getPointPrice() * itemOrderRequest.getQuantity();
                continue;
            }

            double price = request.isDiscount() ? item.getDiscountPrice() : item.getRegularPrice();
            double itemSubtotal = price * quantity;

            totalMoneyPrice += itemSubtotal;

            // Compute item-level tax and gratuity
            double itemTax = itemSubtotal * item.getTaxPercent();
            double itemGratuity = itemSubtotal * item.getGratuityPercent();

            totalTax += itemTax;
            totalGratuity += itemGratuity;
        }

        totalTax = Math.round(totalTax * 100) / 100.0;
        totalGratuity = Math.round(totalGratuity * 100) / 100.0;

        // Compute base before service fee
        double baseAmount = totalMoneyPrice + totalTax + totalGratuity;

        // Fetch and apply service fee from config
        Config feeConfig = configRepository.findByKey("service_fee")
                .orElseThrow(() -> new RuntimeException("Missing service_fee config"));
        String[] parts = feeConfig.getValue().replace(" ", "").split("\\+");
        double percent = Double.parseDouble(parts[0]);
        double flat = Double.parseDouble(parts[1]);

        totalServiceFee = Math.round((percent * baseAmount + flat) * 100) / 100.0;

        // Add to final total
        finalTotal = baseAmount + totalServiceFee;

        // Check if customer has enough points to cover the point-based portion
        if (!pointService.customerHasRequiredBalance(totalPointsPrice, baseAmount, request.getCustomerId(), merchantId)) {
            return OrderResponse.builder()
                    .message("Insufficient points.")
                    .messageType("error")
                    .totalGratuity(totalGratuity)
                    .totalServiceFee(totalServiceFee)
                    .totalTax(totalTax)
                    .totalPrice(totalMoneyPrice)
                    .totalPointPrice(totalPointsPrice)
                    .items(itemOrderResponses)
                    .name(customerName)
                    .build();
        }

        // If using in-app payments, attempt to process via Stripe
        if (request.isInAppPayments()) {
            try {
                stripeService.processOrder(finalTotal, request.getCustomerId(), merchantId, totalServiceFee);
            } catch (StripeException exception) {
                // Log Stripe exception
                System.out.println("Stripe error: " + exception.getMessage());
                return OrderResponse.builder()
                        .message(
                                "There was an issue processing your payment. Please check your card details or try another payment method.")
                        .messageType("error")
                        .totalGratuity(totalGratuity)
                        .totalServiceFee(totalServiceFee)
                        .totalTax(totalTax)
                        .totalPrice(totalMoneyPrice)
                        .totalPointPrice(totalPointsPrice)
                        .items(itemOrderResponses)
                        .name(customerName)
                        .build();
            } catch (InvalidStripeChargeException exception) {
                System.out.println(exception.getMessage());
                return OrderResponse.builder()
                        .message(
                                "There was an issue processing your payment. Please check your card details or try another payment method.")
                        .messageType("error")
                        .totalGratuity(totalGratuity)
                        .totalServiceFee(totalServiceFee)
                        .totalTax(totalTax)
                        .totalPrice(totalMoneyPrice)
                        .totalPointPrice(totalPointsPrice)
                        .items(itemOrderResponses)
                        .name(customerName)
                        .build();
            }
        }

        // Deduct the points
        pointService.chargeCustomer(totalPointsPrice, baseAmount, request.getCustomerId(), merchantId);

        // Return a successful response
        return OrderResponse.builder()
                .message("Order processed successfully")
                .messageType("success")
                 .totalGratuity(totalGratuity)
                .totalServiceFee(totalServiceFee)
                .totalTax(totalTax)
                .totalPrice(totalMoneyPrice)
                .totalPointPrice(totalPointsPrice)
                .items(itemOrderResponses)
                .name(customerName)
                .build();
    }

}