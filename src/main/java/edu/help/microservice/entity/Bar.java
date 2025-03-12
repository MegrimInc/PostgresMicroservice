package edu.help.microservice.entity;


import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;


import edu.help.microservice.util.HashMapConverter;
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
@Table(name = "bars")
public class Bar implements Serializable {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bar_id", nullable = false)
    private Integer barId;


    @Column(name = "bar_city", nullable = false, length = 255)
    private String barCity;


    @Column(name = "bar_state", nullable = false, length = 255)
    private String barState;


    @Column(name = "bar_address", nullable = false, length = 255)
    private String barAddress;


    @Column(name = "bar_country", nullable = false, length = 255)
    private String barCountry;


    @Column(name = "tag_image", nullable = false, length = 255)
    private String tagImage;


    @Column(name = "bar_image", nullable = false, length = 255)
    private String barImage;


    @Column(name = "bar_email", nullable = false, length = 255)
    private String barEmail;


    @Column(name = "bar_name", nullable = false, length = 255)
    private String barName;


    @Column(name = "bar_tag", nullable = false, length = 255)
    private String barTag;


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



