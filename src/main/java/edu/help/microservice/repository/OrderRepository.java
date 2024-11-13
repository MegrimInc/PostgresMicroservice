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

    // Find orders by user ID for order history
    List<Order> findByUserId(Integer userId);

    // Find unclaimed orders by station
    @Query("SELECT o FROM Order o WHERE o.station = :station AND o.tipsClaimed = false")
    List<Order> findUnclaimedOrdersByStation(Character station);

    // Update tipsClaimed for orders after claiming tips
    @Modifying
    @Transactional
    @Query("UPDATE Order o SET o.tipsClaimed = true WHERE o.station = :station AND o.tipsClaimed = false")
    void markTipsAsClaimedForStation(Character station);
}
