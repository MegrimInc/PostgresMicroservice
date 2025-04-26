package edu.help.microservice.service;

import edu.help.microservice.dto.ItemCountDTO;
import edu.help.microservice.entity.Item;
import edu.help.microservice.repository.ItemRepository;
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

    private final ItemRepository  itemRepository;
    private final OrderRepository orderRepository;
    private final PointService pointService;

    public void saveOrder(OrderDTO orderDTO) {
        Order order = new Order();

        // Map fields from OrderDTO to Order entity
        order.setMerchantId(orderDTO.getMerchantId());
        order.setCustomerId(orderDTO.getUserId());

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
        order.setTip(orderDTO.getTip());
        order.setInAppPayments(orderDTO.isInAppPayments());
        order.setStatus(orderDTO.getStatus());
        order.setTerminal(orderDTO.getTerminal());
        order.setPointOfSale(orderDTO.isPointOfSale());
        order.setClaimer(null);

        // Save the order to the database
        orderRepository.save(order);
        pointService.rewardPointsForOrder(order);
    }

    @Transactional(readOnly = true)
    public List<Order> getUnclaimedTips(int merchantId, String station) {
        return orderRepository.findUnclaimedTipsByMerchantIdAndStation(merchantId, station);
    }

    @Transactional
    public void claimTipsForOrders(List<Order> orders, String stationName) {
        List<Integer> orderIds = orders.stream().map(Order::getOrderId).toList();
        orderRepository.updateTipsClaimed(stationName, orderIds);
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
        Page<Order> page = orderRepository.findByMerchantIdAndTimestampLessThanEqualOrderByTimestampDesc(merchantId, startingInstant, pageable);
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
        List<Item> items = itemRepository.findAllItemsByMerchantIdExcludingFields(merchantId);

        // 2. Fetch all orders for the merchant
        List<Order> orders = orderRepository.findByMerchantId(merchantId);

        // 3. Map: itemId → [dollarCount, pointCount]
        Map<Integer, int[]> itemStats = new HashMap<>();

        for (Order order : orders) {
            for (Order.ItemOrder itemOrder : order.getItems()) {
                int id = itemOrder.getItemId();
                int quantity = itemOrder.getQuantity();
                String payment = itemOrder.getPaymentType();

                int[] stats = itemStats.getOrDefault(id, new int[]{0, 0});
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
            int itemId = item.getId();
            String name = item.getName();
            double regularPrice = item.getRegularPrice();

            int[] stats = itemStats.getOrDefault(itemId, new int[]{0, 0});
            int soldWithDollars = stats[0];
            int soldWithPoints = stats[1];
            int totalSold = soldWithDollars + soldWithPoints;

            result.add(new ItemCountDTO(itemId, name, regularPrice, soldWithDollars, soldWithPoints, totalSold));
        }

        // Sort by totalSold DESC
        result.sort((a, b) -> Integer.compare(b.getTotalSold(), a.getTotalSold()));
        return result;

    }

}