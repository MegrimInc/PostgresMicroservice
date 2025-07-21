package edu.help.microservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeShiftSummary {
    private int employeeId;
    private String name;
    private double revenue;
    private double gratuity;
    private int points;
}