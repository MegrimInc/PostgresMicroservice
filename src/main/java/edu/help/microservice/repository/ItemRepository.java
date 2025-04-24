package edu.help.microservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.help.microservice.entity.Item;

public interface ItemRepository extends JpaRepository<Item, Integer> {

    List<Item> findByItemIdIn(List<Integer> itemIds);

    // Query to find all items by merchantId, excluding specific fields
    @Query("SELECT new edu.help.microservice.entity.Item(d.itemId, d.merchantId, d.itemName, d.alcoholContent, d.itemImage, d.itemTags, d.description, d.point, d.singlePrice, d.singleHappyPrice, d.doublePrice, d.doubleHappyPrice) " +
       "FROM Item d WHERE d.merchantId = :merchantId")
    List<Item> findAllItemsByMerchantIdExcludingFields(@Param("merchantId") Integer merchantId);
}