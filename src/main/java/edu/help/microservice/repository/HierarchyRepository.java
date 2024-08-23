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

    @Query("SELECT h FROM Hierarchy h WHERE h.path = :path")
    Hierarchy findHierarchyByPath(@Param("path") String path);

    @Modifying
    @Transactional
    @Query("UPDATE Hierarchy h SET h.rank = h.rank + 1 WHERE h.path = :path")
    void incrementRank(@Param("path") String path);

    @Modifying
    @Transactional
    @Query(
        value = "INSERT INTO hierarchy (path, status, user_id, rank, claimer) VALUES (CAST(:path AS ltree), :status, :userId, 0, :claimer)",
        nativeQuery = true
    )
    void insertLtreePath(@Param("path") String path, @Param("status") String status, @Param("userId") int userId, @Param("claimer") String claimer);

    @Query(value = "INSERT INTO hierarchy (path, status, user_id, rank, claimer) VALUES (:path, :status, :userId, 0, :claimer)", nativeQuery = true)
    void insertDefaultOrderHierarchyForCreateHierarchy(@Param("path") String path,
                                                       @Param("status") String status,
                                                       @Param("userId") int userId,
                                                       @Param("claimer") String claimer);

    @Query(
        value = "SELECT rank FROM hierarchy WHERE path = CAST(:path AS ltree)",
        nativeQuery = true
    )
    Integer findRankForCreateHierarchy(@Param("path") String path);

    @Modifying
    @Transactional
    @Query(
        value = "UPDATE hierarchy SET rank = :rank WHERE path = CAST(:path AS ltree)",
        nativeQuery = true
    )
    void updateRankForCreateHierarchy(@Param("path") String path, @Param("rank") int rank);

    @Modifying
    @Transactional
    @Query(
        value = "INSERT INTO hierarchy (path, status, user_id, rank, claimer) VALUES (CAST(:path AS ltree), :status, :userId, :rank, :claimer)",
        nativeQuery = true
    )
    void insertLtreePathForCreateHierarchy(@Param("path") String path, @Param("status") String status, @Param("userId") int userId, @Param("rank") int rank, @Param("claimer") String claimer);
}
