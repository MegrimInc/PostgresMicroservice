// OrderRepository.java
package edu.help.microservice.repository;

import edu.help.microservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Integer> {

    @Query("SELECT o FROM Order o WHERE o.barId = :barId AND o.station = :station AND o.tipsClaimed IS NULL AND o.tip > 0")
    List<Order> findUnclaimedTipsByBarIdAndStation(@Param("barId") int barId, @Param("station") String station);

    @Modifying
    @Query("UPDATE Order o SET o.tipsClaimed = :bartenderName WHERE o.orderId IN :orderIds")
    void updateTipsClaimed(@Param("bartenderName") String bartenderName, @Param("orderIds") List<Integer> orderIds);
    
    
    
}
