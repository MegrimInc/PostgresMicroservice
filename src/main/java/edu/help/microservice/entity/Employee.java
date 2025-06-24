package edu.help.microservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "employee")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long employeeId;

    @Column(nullable = false)
    private Integer merchantId;

    @Column(nullable = false)
    private String fullName;

    private String imageUrl;

    private String email;

    @Column(nullable = false)
    private Boolean acceptingOrders = false;

    @Column(nullable = false, length = 10)
    private String pinCode;
}
