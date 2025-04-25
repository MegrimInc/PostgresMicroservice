package edu.help.microservice.entity;

import java.sql.Timestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "signup")
@Data
@NoArgsConstructor
public class SignUp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer Id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "passcode", nullable = true)
    private String passcode;  // Password hash, nullable

    @Column(name = "verification_code", nullable = true)
    private String verificationCode;  // Verification code hash, nullable

    @Column(name = "is_merchant", nullable = false)
    private Boolean isMerchant;

    @Column(name = "expiry_timestamp", nullable = true)
    private Timestamp expiryTimestamp;

    // Optional one-to-one relationship with Customer
    @ToString.Exclude 
    @OneToOne(optional = true, cascade = CascadeType.ALL)
    @JoinColumn(name = "customer_id", referencedColumnName = "customer_id")
    private Customer customer;

    // Optional one-to-one relationship with Merchant
    @OneToOne(optional = true, cascade = CascadeType.ALL)
    @JoinColumn(name = "merchant_id", referencedColumnName = "merchant_id")
    private Merchant merchant;
}