package edu.help.microservice.repository;

import edu.help.microservice.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface TagRepository extends JpaRepository<Tag, Integer> {
    
    @Query(value = "SELECT * FROM tags WHERE category_path::text LIKE 'static_tags%'", nativeQuery = true)
    List<Tag> findByCategoryPathPattern();

    @Query(value = "SELECT item_id FROM merchant_inventory WHERE category_id = :categoryId AND merchant_id = :merchantId", nativeQuery = true)
    List<Integer> findItemIdsByCategoryIdAndMerchantId(@Param("categoryId") Integer categoryId, @Param("merchantId") Integer merchantId);
}