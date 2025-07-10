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
    private Integer employeeId;

    @Column(nullable = false)
    private Integer merchantId;

    @Column(nullable = false)
    private String name;

    private String imageUrl;

    private String email;
}
