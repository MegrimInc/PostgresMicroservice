package edu.help.microservice.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import edu.help.microservice.dto.EmployeeShiftSummary;
import edu.help.microservice.entity.Employee;
import edu.help.microservice.repository.EmployeeRepository;
import edu.help.microservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import edu.help.microservice.entity.Order;

@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final OrderRepository orderRepository;

    public List<EmployeeShiftSummary> getAllEmployeeShiftSummaries(int merchantId) {
        List<Employee> employees = employeeRepository.findByMerchantId(merchantId);
        List<EmployeeShiftSummary> results = new ArrayList<>();

        for (Employee emp : employees) {
            Instant cutoff = emp.getShiftTimestamp(); // now stored as UTC Instant
            if (cutoff == null)
                continue;

            List<Order> orders = orderRepository.findByMerchantIdAndEmployeeIdAfterShift(
                    merchantId, emp.getEmployeeId(), cutoff);

            double revenue = 0.0; // (regular + tax), no service fee
            double gratuity = 0.0;
            int points = 0;

            for (Order o : orders) {
                revenue += o.getTotalRegularPrice() + o.getTotalTax();
                gratuity += o.getTotalGratuity();
                points += o.getTotalPointPrice();
            }

            results.add(new EmployeeShiftSummary(
                    emp.getEmployeeId(),
                    emp.getName(),
                    Math.round(revenue * 100) / 100.0,
                    Math.round(gratuity * 100) / 100.0,
                    points));
        }
        return results;
    }

}
