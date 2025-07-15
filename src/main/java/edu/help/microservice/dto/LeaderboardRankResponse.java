package edu.help.microservice.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class LeaderboardRankResponse {
    private int rank;
    private double difference;
    private String rivalFullName;
    private String customerFullName;
}