package edu.help.microservice.entity;


import java.io.Serializable;
import java.time.LocalDate;


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


    @Column(name = "merchant_city", nullable = false, length = 255)
    private String merchantCity;


    @Column(name = "merchant_state", nullable = false, length = 255)
    private String merchantState;


    @Column(name = "merchant_address", nullable = false, length = 255)
    private String merchantAddress;


    @Column(name = "merchant_country", nullable = false, length = 255)
    private String merchantCountry;


    @Column(name = "tag_image", nullable = false, length = 255)
    private String tagImage;


    @Column(name = "merchant_image", nullable = false, length = 255)
    private String merchantImage;


    @Column(name = "merchant_email", nullable = false, length = 255)
    private String merchantEmail;


    @Column(name = "merchant_name", nullable = false, length = 255)
    private String merchantName;


    @Column(name = "merchant_tag", nullable = false, length = 255)
    private String merchantTag;


    @Column(name = "open_hours", length = 255)
    private String openHours;


    @Column(name = "account_id", length = 255)
    private String accountId;


    @Column(name = "sub_id", length = 255)
    private String subId;


    @Column(name = "rewards_sub_id", length = 255) // ✅ Added this to match DB
    private String rewardsSubId;


    @Column(name = "happy_hour_times", length = 5000) // Store as plain text
    private String happyHourTimes;
   




    @Column(name = "start_date")
    private LocalDate startDate;
}


