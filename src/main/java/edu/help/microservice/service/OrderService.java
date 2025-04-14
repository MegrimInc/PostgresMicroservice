package edu.help.microservice.service;

import edu.help.microservice.dto.DrinkCountDTO;
import edu.help.microservice.entity.Drink;
import edu.help.microservice.repository.DrinkRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import edu.help.microservice.dto.OrderDTO;
import edu.help.microservice.entity.Order;
import edu.help.microservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class OrderService {

    private final DrinkRepository  drinkRepository;
    private final OrderRepository orderRepository;
    private final PointService pointService;

    public void saveOrder(OrderDTO orderDTO) {
        Order order = new Order();

        // Map fields from OrderDTO to Order entity
        order.setBarId(orderDTO.getBarId());
        order.setUserId(orderDTO.getUserId());

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

        order.setDrinks(orderDTO.getDrinks());
        order.setTotalPointPrice(orderDTO.getTotalPointPrice());
        order.setTotalRegularPrice(orderDTO.getTotalRegularPrice());
        order.setTip(orderDTO.getTip());
        order.setInAppPayments(orderDTO.isInAppPayments());
        order.setStatus(orderDTO.getStatus());
        order.setStation(orderDTO.getClaimer());
        order.setPointOfSale(orderDTO.isPointOfSale());

        // Set tipsClaimed to null
        order.setTipsClaimed(null);

        // Save the order to the database
        orderRepository.save(order);
        pointService.rewardPointsForOrder(order);
    }

    @Transactional(readOnly = true)
    public List<Order> getUnclaimedTips(int barId, String station) {
        return orderRepository.findUnclaimedTipsByBarIdAndStation(barId, station);
    }

    @Transactional
    public void claimTipsForOrders(List<Order> orders, String bartenderName) {
        List<Integer> orderIds = orders.stream().map(Order::getOrderId).toList();
        orderRepository.updateTipsClaimed(bartenderName, orderIds);
    }

    /**
     * Retrieves all orders for a given bar.
     */
    @Transactional(readOnly = true)
    public List<Order> getAllOrdersForBar(int barId) {
        return orderRepository.findByBarId(barId);
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByDateRange(int barId, Instant start, Instant end) {
        return orderRepository.findByBarIdAndTimestampBetween(barId, start, end);
    }


    @Transactional(readOnly = true)
    public List<Order> getByDay(int barId, Date day) {
        return orderRepository.findByBarIdAndDate(barId, day);
    }

    @Transactional(readOnly = true)
    public List<Order> getFiftyOrders(int barId, Instant startingInstant, int index) {
        Pageable pageable = PageRequest.of(index, 50, Sort.by("timestamp").descending());
        Page<Order> page = orderRepository.findByBarIdAndTimestampLessThanEqualOrderByTimestampDesc(barId, startingInstant, pageable);
        return page.getContent();
    }


    // New method: Get top 5 most ordered drinks for a bar
    @Transactional(readOnly = true)
    public Map<String, Integer> getTop5Drinks(int barId) {
        List<Object[]> results = orderRepository.findTop5DrinksByBarId(barId);
        Map<String, Integer> top5Drinks = new HashMap<>();
        for (Object[] row : results) {
            String drinkName = (String) row[0];
            Integer totalQuantity = ((Number) row[1]).intValue();
            top5Drinks.put(drinkName, totalQuantity);
        }
        return top5Drinks;
    }


    @Transactional(readOnly = true)
    public List<DrinkCountDTO> getAllDrinkCountsForBar(int barId) {
        // 1. Fetch all drinks for the bar
        List<Drink> drinks = drinkRepository.findAllDrinksByBarIdExcludingFields(barId);

        // 2. Fetch all orders for the bar
        List<Order> orders = orderRepository.findByBarId(barId);

        // 3. Map: drinkId → [dollarCount, pointCount]
        Map<Integer, int[]> drinkStats = new HashMap<>();

        for (Order order : orders) {
            for (Order.DrinkOrder drinkOrder : order.getDrinks()) {
                int id = drinkOrder.getDrinkId();
                int quantity = drinkOrder.getQuantity();
                String payment = drinkOrder.getPaymentType();

                int[] stats = drinkStats.getOrDefault(id, new int[]{0, 0});
                if ("points".equalsIgnoreCase(payment)) {
                    stats[1] += quantity;
                } else {
                    stats[0] += quantity; // default to regular/dollars
                }
                drinkStats.put(id, stats);
            }
        }

        // 4. Build DTO list from drinks + stats
        List<DrinkCountDTO> result = new ArrayList<>();
        for (Drink drink : drinks) {
            int drinkId = drink.getDrinkId();
            String name = drink.getDrinkName();
            double doublePrice = drink.getDoublePrice() != null ? drink.getDoublePrice() : 0.0;

            int[] stats = drinkStats.getOrDefault(drinkId, new int[]{0, 0});
            int soldWithDollars = stats[0];
            int soldWithPoints = stats[1];
            int totalSold = soldWithDollars + soldWithPoints;

            result.add(new DrinkCountDTO(drinkId, name, doublePrice, soldWithDollars, soldWithPoints, totalSold));
        }

        // Sort by totalSold DESC
        result.sort((a, b) -> Integer.compare(b.getTotalSold(), a.getTotalSold()));
        return result;

    }

}

