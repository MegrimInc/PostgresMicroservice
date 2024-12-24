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

    @Column(name = "bar_id", nullable = false)
    private Integer barId;

    @Column(name = "bartender_id", nullable = false, length = 50)
    private String bartenderId;

    // This column will store the date/time (hour) of activity
    @Column(name = "activity_date", nullable = false)
    private LocalDateTime activityDate;

    // If you like, you can also add a constructor that doesn't have the ID:
    public Activity(Integer barId, String bartenderId, LocalDateTime activityDate) {
        this.barId = barId;
        this.bartenderId = bartenderId;
        this.activityDate = activityDate;
    }
}
