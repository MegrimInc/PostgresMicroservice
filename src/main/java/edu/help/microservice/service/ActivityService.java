package edu.help.microservice.service;

import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import edu.help.microservice.entity.Activity;
import edu.help.microservice.repository.ActivityRepository;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;

    public boolean alreadyRecordedThisHour(Integer merchantId, String stationId, LocalDateTime hour) {
        return activityRepository.existsByMerchantIdAndMerchanttenderIdAndActivityDate(merchantId, stationId, hour);
    }

    public Activity recordActivity(Integer merchantId, String stationId, LocalDateTime hour) {
        Activity activity = new Activity(merchantId, stationId, hour);
        return activityRepository.save(activity);
    }
}