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

    @Query(
        value = "SELECT rank FROM hierarchy WHERE path = CAST(:path AS ltree)",
        nativeQuery = true
    )
    Integer findRankByPath(@Param("path") String path);

    @Modifying
    @Transactional
    @Query(
        value = "UPDATE hierarchy SET rank = :rank WHERE path = CAST(:path AS ltree)",
        nativeQuery = true
    )
    void updateRank(@Param("path") String path, @Param("rank") int rank);

    @Modifying
    @Transactional
    @Query(
        value = "INSERT INTO hierarchy (path, status, user_id, rank, claimer) VALUES (CAST(:path AS ltree), :status, :userId, :rank, :claimer)",
        nativeQuery = true
    )
    void insertLtreePath(@Param("path") String path, @Param("status") String status, @Param("userId") int userId, @Param("rank") int rank, @Param("claimer") String claimer);
}
