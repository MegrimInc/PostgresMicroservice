package edu.help.microservice.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.help.microservice.entity.Customer;
import edu.help.microservice.repository.CustomerRepository;

@Service
public class PointService {

    @Autowired
    private CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public Map<Integer, Map<Integer, Integer>> getPointsForUser(int userId) {
        Optional<Customer> customerOpt = customerRepository.findById(userId);
        return customerOpt.map(Customer::getPoints).orElse(new HashMap<>());
    }

    // Charge points from a user with quantity-based adjustment
    @Transactional
    public boolean charge(int userId, int barId, int pointsToCharge, int totalQuantity) {
        Optional<Customer> customerOpt = customerRepository.findById(userId);
        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            Map<Integer, Map<Integer, Integer>> pointsMap = getOrInitializePoints(customer);

            int currentPoints = pointsMap.getOrDefault(userId, new HashMap<>()).getOrDefault(barId, 0);

            if (currentPoints < pointsToCharge) {
                System.out.println("Insufficient points for charge.");
                return false;  // Not enough points, transaction fails
            }

            // Subtract pointsToCharge and add totalQuantity * 75 points back
            int updatedPoints = currentPoints - pointsToCharge + (totalQuantity * 75);
            pointsMap.get(userId).put(barId, updatedPoints);

            customer.setPoints(pointsMap);
            customerRepository.save(customer);  // Save updated customer entity

            System.out.println("Points charged: " + pointsToCharge);
            System.out.println("Points added from drinks: " + (totalQuantity * 75));
            return true;  // Charge successful
        }
        return false;  // User not found, charge fails
    }

    // Refund points with quantity-based adjustment
    @Transactional
    public void refund(int userId, int barId, int pointsToRefund, int totalQuantity) {
        Optional<Customer> customerOpt = customerRepository.findById(userId);
        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            Map<Integer, Map<Integer, Integer>> pointsMap = getOrInitializePoints(customer);

            int currentPoints = pointsMap.getOrDefault(userId, new HashMap<>()).getOrDefault(barId, 0);

            // Add pointsToRefund and subtract totalQuantity * 75 points
            int updatedPoints = currentPoints + pointsToRefund - (totalQuantity * 75);
            pointsMap.get(userId).put(barId, updatedPoints);

            customer.setPoints(pointsMap);
            customerRepository.save(customer); // Save updated customer entity

            System.out.println("Points refunded: " + pointsToRefund);
            System.out.println("Points removed from drinks: " + (totalQuantity * 75));
        }
    }
    

    // Add points for a given user ID and bar ID
    @Transactional
    public void addPoints(int userId, int barId, int pointsToAdd) {
        Optional<Customer> customerOpt = customerRepository.findById(userId);
        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            Map<Integer, Map<Integer, Integer>> pointsMap = getOrInitializePoints(customer);

            // Multiply the pointsToAdd by 75 before adding
            int finalPointsToAdd = pointsToAdd * 75;

            pointsMap.computeIfAbsent(userId, k -> new HashMap<>()).merge(barId, finalPointsToAdd, Integer::sum);

            customer.setPoints(pointsMap);  // Set the updated points map
            customerRepository.save(customer);  // Save updated customer entity
        }
    }

    // Subtract points for a given user ID and bar ID
    @Transactional
    public void subtractPoints(int userId, int barId, int pointsToSubtract) {
        Optional<Customer> customerOpt = customerRepository.findById(userId);
        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            Map<Integer, Map<Integer, Integer>> pointsMap = getOrInitializePoints(customer);

            // Multiply the pointsToSubtract by 75 before subtracting
            int finalPointsToSubtract = pointsToSubtract * 75;

            pointsMap.computeIfPresent(userId, (k, barMap) -> {
                barMap.merge(barId, -finalPointsToSubtract, Integer::sum);
                if (barMap.get(barId) <= 0) {
                    barMap.remove(barId);  // Remove bar ID entry if points are zero or negative
                }
                return barMap.isEmpty() ? null : barMap;  // Remove user entry if no bars left
            });

            customer.setPoints(pointsMap);  // Set the updated points map
            customerRepository.save(customer);  // Save updated customer entity
        }
    }


    // Helper method to initialize points map if it is null
    private Map<Integer, Map<Integer, Integer>> getOrInitializePoints(Customer customer) {
        Map<Integer, Map<Integer, Integer>> pointsMap = customer.getPoints();
        return (pointsMap != null) ? pointsMap : new HashMap<>();
    }
}