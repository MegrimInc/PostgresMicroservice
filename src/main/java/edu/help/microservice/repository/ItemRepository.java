package edu.help.microservice.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import edu.help.microservice.entity.Item;

public interface ItemRepository extends JpaRepository<Item, Integer> {

    // Fixed method name to match "id" field (not "itemId")
    List<Item> findByItemIdIn(List<Integer> itemIds);

    // Fixed JPQL query to match your entity's fields and constructor
    @Query("SELECT new edu.help.microservice.entity.Item(" +
           "d.merchantId, d.itemId, d.name, d.image, d.categories, " +
           "d.description, d.pointPrice, d.regularPrice, d.discountPrice) " +
           "FROM Item d WHERE d.merchantId = :merchantId")
    List<Item> findAllItemsByMerchantIdExcludingFields(@Param("merchantId") Integer merchantId);
}