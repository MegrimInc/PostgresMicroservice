package edu.help.microservice.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

// If you prefer to name your schema or table differently,
// adjust the @Table annotation
@Entity
@Table(name = "activity")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_id")
    private Long activityId;

    @Column(name = "merchant_id", nullable = false)
    private Integer merchantId;

    @Column(name = "station_id", nullable = false, length = 50)
    private String stationId;

    // This column will store the date/time (hour) of activity
    @Column(name = "activity_date", nullable = false)
    private LocalDateTime activityDate;

    // If you like, you can also add a constructor that doesn't have the ID:
    public Activity(Integer merchantId, String stationId, LocalDateTime activityDate) {
        this.merchantId = merchantId;
        this.stationId = stationId;
        this.activityDate = activityDate;
    }
}