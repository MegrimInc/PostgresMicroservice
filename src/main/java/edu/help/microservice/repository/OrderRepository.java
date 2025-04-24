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

    @Query("SELECT o FROM Order o WHERE o.merchantId = :merchantId AND o.station = :station AND o.tipsClaimed IS NULL AND o.tip > 0")
    List<Order> findUnclaimedTipsByMerchantIdAndStation(@Param("merchantId") int merchantId, @Param("station") String station);

    @Modifying
    @Query("UPDATE Order o SET o.tipsClaimed = :stationName WHERE o.orderId IN :orderIds")
    void updateTipsClaimed(@Param("stationName") String stationName, @Param("orderIds") List<Integer> orderIds);

    @Query("SELECT o FROM Order o WHERE o.merchantId = :merchantId")
    List<Order> findByMerchantId(@Param("merchantId") int merchantId);

    @Query("SELECT o FROM Order o WHERE o.merchantId = :merchantId AND o.timestamp BETWEEN :start AND :end")
    List<Order> findByMerchantIdAndTimestampBetween(@Param("merchantId") int merchantId, @Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT o FROM Order o WHERE o.merchantId = :merchantId AND o.timestamp <= :startingInstant ORDER BY o.timestamp DESC")
    org.springframework.data.domain.Page<Order> findByMerchantIdAndTimestampLessThanEqualOrderByTimestampDesc(
            @Param("merchantId") int merchantId, @Param("startingInstant") Instant startingInstant, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.merchantId = :merchantId AND FUNCTION('DATE', o.timestamp) = :day")
    List<Order> findByMerchantIdAndDate(@Param("merchantId") int merchantId, @Param("day") Date day);


    @Query(value = "SELECT d->>'itemName' as itemName, SUM((d->>'quantity')::int) as totalQuantity " +
            "FROM orders, jsonb_array_elements(items) d " +
            "WHERE merchant_id = :merchantId " +
            "GROUP BY d->>'itemName' " +
            "ORDER BY totalQuantity DESC " +
            "LIMIT 5", nativeQuery = true)
    List<Object[]> findTop5ItemsByMerchantId(@Param("merchantId") int merchantId);

    @Query(value = "SELECT d->>'itemName' AS itemName, SUM((d->>'quantity')::int) AS totalQuantity " +
            "FROM orders, jsonb_array_elements(items) d " +
            "WHERE merchant_id = :merchantId " +
            "GROUP BY d->>'itemName' " +
            "ORDER BY totalQuantity DESC", nativeQuery = true)
    List<Object[]> findAllItemCountsByMerchantId(@Param("merchantId") int merchantId);

}