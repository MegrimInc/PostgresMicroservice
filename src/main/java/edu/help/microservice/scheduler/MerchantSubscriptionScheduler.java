package edu.help.microservice.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MerchantSubscriptionScheduler {

    @Scheduled(cron = "0 0 0 1 * ?")
    public void chargeMonthlySubscriptions() {
        // Add logic here
    }
}
