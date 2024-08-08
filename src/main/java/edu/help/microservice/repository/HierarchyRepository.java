package edu.help.microservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import edu.help.microservice.entity.Hierarchy;

@Repository
public interface HierarchyRepository extends JpaRepository<Hierarchy, Long> {

    @Modifying
    @Transactional
    @Query(
        value = "INSERT INTO hierarchy (path, status, user_id) VALUES (CAST(:path AS ltree), :status, :userId)",
        nativeQuery = true
    )
    void insertLtreePath(@Param("path") String path, @Param("status") int status, @Param("userId") int userId);
}
