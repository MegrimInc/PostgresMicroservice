package edu.help.microservice.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

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
    public List<Order> getAllOrdersForBar(int barId) {
        return orderRepository.findByBarId(barId);
    }

    /**
     * Retrieves orders for a given bar within the specified date range.
     */
    public List<Order> getOrdersByDateRange(int barId, Instant start, Instant end) {
        return orderRepository.findByBarIdAndTimestampBetween(barId, start, end);
    }

    /**
     * Retrieves 50 orders for the given bar that have a timestamp less than or equal to the provided startingInstant.
     * The results are ordered descending by timestamp. The index parameter allows for pagination (e.g. index=0 returns the first 50).
     */
    public List<Order> getFiftyOrders(int barId, Instant startingInstant, int index) {
        Pageable pageable = PageRequest.of(index, 50, Sort.by("timestamp").descending());
        return orderRepository.findByBarIdAndTimestampLessThanEqualOrderByTimestampDesc(barId, startingInstant, pageable).getContent();
    }
}

