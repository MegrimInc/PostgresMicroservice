package edu.help.microservice.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import edu.help.microservice.entity.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.help.microservice.entity.Customer;
import edu.help.microservice.repository.CustomerRepository;

@RequiredArgsConstructor
@Service
public class PointService {
    private static final int DRINK_QUANTITY_MULTIPLIER = 75;

    private final CustomerRepository customerRepository;

    public boolean customerHasRequiredBalance(int needed, int customerId, int barId) {
        int customerPoints = getPointsAtBar(customerId, barId);;
        return needed <= customerPoints;
    }

    public void chargeCustomer(int points, int customerId, int barId) {
        Optional<Customer> customerOpt = customerRepository.findById(customerId);
        if (customerOpt.isEmpty())
            return;
        if (!customerHasRequiredBalance(points, customerId, barId))
            return;

        var pointsMap = getPointsForCustomer(customerId);
        if (pointsMap == null)
            return;

        if (!pointsMap.containsKey(barId))
            pointsMap.put(barId, 0);
        pointsMap.put(barId, pointsMap.get(barId) - points);
        customerRepository.save(customerOpt.get());
    }

    public void rewardCustomer(int drinkQuantity, int customerId, int barId) {
        Optional<Customer> customerOpt = customerRepository.findById(customerId);
        if (customerOpt.isEmpty())
            return;

        var pointsMap = getPointsForCustomer(customerId);
        if (pointsMap == null)
            return;

        pointsMap.put(barId, pointsMap.get(barId) + drinkQuantity * DRINK_QUANTITY_MULTIPLIER);
        customerRepository.save(customerOpt.get());
    }

    public Map<Integer, Map<Integer, Integer>> getPointsForCustomerTempForEndpoint(int userId) {
        Optional<Customer> customerOpt = customerRepository.findById(userId);
        return customerOpt.map(Customer::getPoints).orElse(null);
    }

    private Map<Integer, Integer> getPointsForCustomer(int userId) {
        Optional<Customer> customerOpt = customerRepository.findById(userId);
        if (customerOpt.isEmpty())
            return null;

        Customer customer = customerOpt.get();
        if (!customer.getPoints().containsKey(userId)) {
            customer.getPoints().put(userId, new HashMap<>());
            customerRepository.save(customer);
        }

        return customer.getPoints().get(userId);
    }

    public void rewardPointsForOrder(Order order) {
        if (order.getStatus().equals("canceled"))
            return;

        int totalDrinkQuantity = 0;
        for (Order.DrinkOrder drinkOrder : order.getDrinks())
            totalDrinkQuantity += drinkOrder.getQuantity();
        rewardCustomer(totalDrinkQuantity, order.getUserId(), order.getBarId());
    }

    private int getPointsAtBar(int customerId, int barId) {
        var userPoints = getPointsForCustomer(customerId);
        if (userPoints == null)
            return -1;

        if (!userPoints.containsKey(barId))
            userPoints.put(barId, 0);
        return userPoints.get(barId);
    }
}