package edu.help.microservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import edu.help.microservice.entity.Hierarchy;

@Repository
public interface HierarchyRepository extends JpaRepository<Hierarchy, String> {
    
    @Modifying
    @Transactional
    @Query(
        value = "INSERT INTO hierarchy (path, status, user_id, claimer) VALUES (CAST(:path AS ltree), :status, :userId, :claimer)",
        nativeQuery = true
    )
    void insertLtreePathForCreateHierarchy(@Param("path") String path, @Param("status") String status, @Param("userId") int userId,  @Param("claimer") String claimer);
}
