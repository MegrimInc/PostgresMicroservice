// OrderRepository.java
package edu.help.microservice.repository;

import edu.help.microservice.entity.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Date;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Integer> {

    @Query("SELECT o FROM Order o WHERE o.barId = :barId AND o.station = :station AND o.tipsClaimed IS NULL AND o.tip > 0")
    List<Order> findUnclaimedTipsByBarIdAndStation(@Param("barId") int barId, @Param("station") String station);

    @Modifying
    @Query("UPDATE Order o SET o.tipsClaimed = :bartenderName WHERE o.orderId IN :orderIds")
    void updateTipsClaimed(@Param("bartenderName") String bartenderName, @Param("orderIds") List<Integer> orderIds);

    @Query("SELECT o FROM Order o WHERE o.barId = :barId")
    List<Order> findByBarId(@Param("barId") int barId);

    @Query("SELECT o FROM Order o WHERE o.barId = :barId AND o.timestamp BETWEEN :start AND :end")
    List<Order> findByBarIdAndTimestampBetween(@Param("barId") int barId, @Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT o FROM Order o WHERE o.barId = :barId AND o.timestamp <= :startingInstant ORDER BY o.timestamp DESC")
    org.springframework.data.domain.Page<Order> findByBarIdAndTimestampLessThanEqualOrderByTimestampDesc(
            @Param("barId") int barId, @Param("startingInstant") Instant startingInstant, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.barId = :barId AND FUNCTION('DATE', o.timestamp) = :day")
    List<Order> findByBarIdAndDate(@Param("barId") int barId, @Param("day") Date day);


    @Query(value = "SELECT d->>'drinkName' as drinkName, SUM((d->>'quantity')::int) as totalQuantity " +
            "FROM orders, jsonb_array_elements(drinks) d " +
            "WHERE bar_id = :barId " +
            "GROUP BY d->>'drinkName' " +
            "ORDER BY totalQuantity DESC " +
            "LIMIT 5", nativeQuery = true)
    List<Object[]> findTop5DrinksByBarId(@Param("barId") int barId);

    @Query(value = "SELECT d->>'drinkName' AS drinkName, SUM((d->>'quantity')::int) AS totalQuantity " +
            "FROM orders, jsonb_array_elements(drinks) d " +
            "WHERE bar_id = :barId " +
            "GROUP BY d->>'drinkName' " +
            "ORDER BY totalQuantity DESC", nativeQuery = true)
    List<Object[]> findAllDrinkCountsByBarId(@Param("barId") int barId);

}
