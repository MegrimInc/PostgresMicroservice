package edu.help.microservice.entity;


import java.io.Serializable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "merchants")
public class Merchant implements Serializable {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "merchant_id", nullable = false)
    private Integer merchantId;


    @Column(name = "shift_timestamp")
    private java.time.LocalDateTime shiftTimestamp;

    @Column(name = "city", nullable = false, length = 255)
    private String city;


    @Column(name = "state_or_province", nullable = false, length = 255)
    private String stateOrProvince;


    @Column(name = "address", nullable = false, length = 255)
    private String address;


    @Column(name = "zip_code", nullable = false, length = 255)
    private String zipCode;


    @Column(name = "country", nullable = false, length = 255)
    private String country;


    @Column(name = "logo_image", nullable = true, length = 255)
    private String logoImage;


    @Column(name = "store_image", nullable = true, length = 255)
    private String storeImage;


    @Column(name = "name", nullable = false, length = 255)
    private String name;


    @Column(name = "nickname", nullable = false, length = 255)
    private String nickname;


    @Column(name = "stripe_verification_status", nullable = false)
    private String stripeVerificationStatus;


    @Column(name = "account_id", length = 255)
    private String accountId;

    @Column(name = "discount_schedule", length = 5000) // Store as plain text
    private String discountSchedule;


    @Column(name = "bonus") // Store as plain text
    private int bonus;

     @Column(name = "is_live_account")
    private Boolean isLiveAccount;
}


