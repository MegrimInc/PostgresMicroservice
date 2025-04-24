package edu.help.microservice.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import edu.help.microservice.entity.Merchant;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, Integer> {

    /**
     * Finds a Merchant by its email.
     */
    Optional<Merchant> findByMerchantEmail(String merchant_email);

    /**
     * Updates the 'start_date' column (type DATE or TIMESTAMP) for a given merchant ID.
     * Uses a native SQL query.
     */
    @Modifying
    @Transactional
    @Query(value = "UPDATE merchants SET start_date = :date WHERE merchant_id = :merchantId", nativeQuery = true)
    int updateStartDate(@Param("merchantId") int merchantId, @Param("date") LocalDate date);

}