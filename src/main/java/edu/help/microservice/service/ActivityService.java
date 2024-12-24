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

    public boolean alreadyRecordedThisHour(Integer barId, String bartenderId, LocalDateTime hour) {
        return activityRepository.existsByBarIdAndBartenderIdAndActivityDate(barId, bartenderId, hour);
    }

    public Activity recordActivity(Integer barId, String bartenderId, LocalDateTime hour) {
        Activity activity = new Activity(barId, bartenderId, hour);
        return activityRepository.save(activity);
    }
}
