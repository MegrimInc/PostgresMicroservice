package edu.help.microservice.repository;

import edu.help.microservice.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    List<Category> findByMerchantId(Integer merchantId);

    List<Category> findAllByMerchantId(Integer merchantId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Category c WHERE c.categoryId = :id AND c.merchantId = :merchantId")
    void deleteByIdAndMerchantId(@Param("id") Integer id,
                                 @Param("merchantId") Integer merchantId);
}

