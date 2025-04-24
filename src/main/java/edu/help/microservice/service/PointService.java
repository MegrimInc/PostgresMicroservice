package edu.help.microservice.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import edu.help.microservice.entity.Customer;
import edu.help.microservice.entity.Order;
import edu.help.microservice.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class PointService {

    private final CustomerRepository customerRepository;

    public boolean customerHasRequiredBalance(int needed, int customerId, int merchantId) {
        int customerPoints = getPointsAtMerchant(customerId, merchantId);
        return needed <= customerPoints;
    }

    public void chargeCustomer(int points, int customerId, int merchantId) {
        Optional<Customer> customerOpt = customerRepository.findById(customerId);
        if (customerOpt.isEmpty())
            return;
        if (!customerHasRequiredBalance(points, customerId, merchantId))
            return;

        var pointsMap = getPointsForCustomer(customerId);
        if (pointsMap == null)
            return;

        if (!pointsMap.containsKey(merchantId))
            pointsMap.put(merchantId, 0);
        pointsMap.put(merchantId, pointsMap.get(merchantId) - points);
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

        Optional<Customer> customerOpt = customerRepository.findById(order.getUserId());
        if (customerOpt.isEmpty())
            return;

        var pointsMap = getPointsForCustomer(order.getUserId());
        if (pointsMap == null)
            return;

        if (order.getTotalRegularPrice() <= 0) {
            return;
        }

        double pointsExact = order.getTotalRegularPrice() * 10 * 1.2;
        int rewardPoints = (int) Math.round(pointsExact);

        pointsMap.put(order.getMerchantId(), pointsMap.get(order.getMerchantId()) + rewardPoints);
        customerRepository.save(customerOpt.get());
    }

    private int getPointsAtMerchant(int customerId, int merchantId) {
        var userPoints = getPointsForCustomer(customerId);
        if (userPoints == null)
            return -1;

        if (!userPoints.containsKey(merchantId))
            userPoints.put(merchantId, 0);
        return userPoints.get(merchantId);
    }
}