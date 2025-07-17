package edu.help.microservice.repository;

import edu.help.microservice.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findByMerchantId(Integer merchantId);

    @Modifying
    @Transactional
    @Query("UPDATE Employee e SET e.shiftTimestamp = :ts WHERE e.merchantId = :merchId")
    void resetShiftForAll(@Param("merchId") Integer merchantId, @Param("ts") LocalDateTime ts);


}
