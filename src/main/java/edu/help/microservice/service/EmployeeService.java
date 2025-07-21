package edu.help.microservice.service;

import java.time.ZoneId;
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
            if (emp.getShiftTimestamp() == null) continue;

            List<Order> orders = orderRepository.findByMerchantIdAndEmployeeIdAfterShift(
                    merchantId,
                    emp.getEmployeeId(),
                    emp.getShiftTimestamp().atZone(ZoneId.of("America/New_York")).toInstant()
            );

            double revenue = 0;
            double gratuity = 0;
            int points = 0;

            for (Order o : orders) {
                revenue += o.getTotalRegularPrice() + o.getTotalTax();
                gratuity += o.getTotalGratuity();
                points += o.getTotalPointPrice();
            }

            results.add(new EmployeeShiftSummary(emp.getEmployeeId(), emp.getName(), revenue, gratuity, points));
        }

        return results;
    }
    
}
