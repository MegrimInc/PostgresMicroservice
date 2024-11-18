package edu.help.microservice.repository;

import edu.help.microservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    // Find unclaimed orders by bar ID and station
    @Query("SELECT o FROM Order o WHERE o.barId = :barId AND o.station = :station AND o.tipsClaimed IS NULL")
    List<Order> findUnclaimedOrdersByBarAndStation(Integer barId, Character station);

    // Update orders to mark tips as claimed by bartender name
    @Modifying
    @Transactional
    @Query("UPDATE Order o SET o.tipsClaimed = :bartenderName WHERE o.barId = :barId AND o.station = :station AND o.tipsClaimed IS NULL")
    void markTipsAsClaimedByBartender(Integer barId, Character station, String bartenderName);
}
