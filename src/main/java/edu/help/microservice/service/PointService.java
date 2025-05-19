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

    public boolean customerHasRequiredBalance(int needed, double baseAmount, int customerId, int merchantId) {
        int customerPoints = getPointsAtMerchant(customerId, merchantId);

        // Earned points (e.g. 10 points per $1)
        int earnedPoints = (int) Math.floor(baseAmount * 10);

        int availablePoints = customerPoints + earnedPoints;

        return needed <= availablePoints;
    }

    public void chargeCustomer(int points, double baseAmount, int customerId, int merchantId) {
        Optional<Customer> customerOpt = customerRepository.findById(customerId);
        if (customerOpt.isEmpty())
            return;
        if (!customerHasRequiredBalance(points, baseAmount, customerId, merchantId))
            return;

        var pointsMap = getPointsForCustomer(customerId);
        if (pointsMap == null)
            return;

        pointsMap.putIfAbsent(merchantId, 0);
        int earnedPoints = (int) Math.floor(baseAmount * 10);
        int current = pointsMap.get(merchantId);
        pointsMap.put(merchantId, current - points + earnedPoints);
        customerRepository.save(customerOpt.get());
    }

    public Map<Integer, Map<Integer, Integer>> getPointsForCustomerTempForEndpoint(int customerId) {
        Optional<Customer> customerOpt = customerRepository.findById(customerId);
        return customerOpt.map(Customer::getPoints).orElse(null);
    }

    private Map<Integer, Integer> getPointsForCustomer(int customerId) {
        Optional<Customer> customerOpt = customerRepository.findById(customerId);
        if (customerOpt.isEmpty())
            return null;

        Customer customer = customerOpt.get();
        if (!customer.getPoints().containsKey(customerId)) {
            customer.getPoints().put(customerId, new HashMap<>());
            customerRepository.save(customer);
        }

        return customer.getPoints().get(customerId);
    }

    /**
    * REDACTED METHOD - previously rewarded customers based on final Order details
    */
    public void rewardPointsForOrder(Order order) {

        Optional<Customer> customerOpt = customerRepository.findById(order.getCustomerId());
        if (customerOpt.isEmpty())
            return;

        var pointsMap = getPointsForCustomer(order.getCustomerId());
        if (pointsMap == null)
            return;

        if (order.getTotalRegularPrice() <= 0) {
            return;
        }

        double pointsExact = order.getTotalRegularPrice() * 10;
        int rewardPoints = (int) Math.round(pointsExact);

        pointsMap.put(order.getMerchantId(), pointsMap.get(order.getMerchantId()) + rewardPoints);
        customerRepository.save(customerOpt.get());
    }

    private int getPointsAtMerchant(int customerId, int merchantId) {
        var customerPoints = getPointsForCustomer(customerId);
        if (customerPoints == null)
            return -1;

        if (!customerPoints.containsKey(merchantId))
            customerPoints.put(merchantId, 0);
        return customerPoints.get(merchantId);
    }
}