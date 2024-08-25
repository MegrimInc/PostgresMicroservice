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

    @Query("SELECT CAST(subpath(path, nlevel(:path), 1) AS text)::integer " +
       "FROM Hierarchy h " +
       "WHERE subpath(path, 0, nlevel(:path)) = CAST(:path AS ltree)")
    Integer findQuantityByPath(@Param("path") String path);







    @Modifying
@Transactional
@Query(
    value = "UPDATE hierarchy SET path = CAST(:newPath AS ltree) WHERE path = CAST(:path AS ltree)",
    nativeQuery = true
)
void updateQuantityForSaveOrder(@Param("path") String path, @Param("newPath") String newPath);



@Modifying
@Transactional
@Query(
    value = "INSERT INTO hierarchy (path, status, user_id, rank, claimer) VALUES (CAST(:path AS ltree), :status, :userId, :rank, :claimer)",
    nativeQuery = true
)
void insertLtreePath2(@Param("path") String path, @Param("status") String status, @Param("userId") int userId, @Param("rank") int rank, @Param("claimer") String claimer);

    
    @Query("SELECT h FROM Hierarchy h WHERE h.path = :path")
    Hierarchy findHierarchyByPath(@Param("path") String path);


    

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
