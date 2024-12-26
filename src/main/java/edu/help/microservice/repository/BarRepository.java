package edu.help.microservice.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import edu.help.microservice.entity.Bar;

@Repository
public interface BarRepository extends JpaRepository<Bar, Integer> {

    /**
     * Finds a Bar by its email.
     */
    Optional<Bar> findByBarEmail(String bar_email);

    /**
     * Updates the 'start_date' column (type DATE or TIMESTAMP) for a given bar ID.
     * Uses a native SQL query.
     */
    @Modifying
    @Transactional
    @Query(value = "UPDATE bars SET start_date = :date WHERE bar_id = :barId", nativeQuery = true)
    int updateStartDate(@Param("barId") int barId, @Param("date") LocalDate date);

}
