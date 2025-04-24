package edu.help.microservice.repository;

import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.help.microservice.entity.Activity;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {

    // If you want to check if a row already exists for (merchantID, stationID, hour):
    boolean existsByMerchantIdAndMerchanttenderIdAndActivityDate(Integer merchantId, String stationId, LocalDateTime activityDate);

    // You could add more custom queries if needed
}