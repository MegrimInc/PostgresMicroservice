package edu.help.microservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Entity
@Table(name = "signup")
@Data
@NoArgsConstructor
public class SignUp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer ID;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "passcode")
    private String passcode;

    @Column(name = "is_bar", nullable = false)
    private Boolean isBar = false;

    @Column(name = "expiry_Timestamp")
    private Timestamp expiryTimestamp;

    // Optional one-to-one relationship with Customer
    @OneToOne(optional = true, cascade = CascadeType.ALL)
    @JoinColumn(name = "customerID", referencedColumnName = "customerID")
    private Customer customer;

    // Optional one-to-one relationship with Bar
    @OneToOne(optional = true, cascade = CascadeType.ALL)
    @JoinColumn(name = "bar_id", referencedColumnName = "bar_id")
    private Bar bar;
}

