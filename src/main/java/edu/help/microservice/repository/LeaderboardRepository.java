package edu.help.microservice.repository;

import edu.help.microservice.entity.Leaderboard;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LeaderboardRepository extends JpaRepository<Leaderboard, Integer> {
    List<Leaderboard> findByMerchantIdOrderByTotalDesc(Integer merchantId);
}