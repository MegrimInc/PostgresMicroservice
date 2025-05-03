package edu.help.microservice.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import edu.help.microservice.entity.Item;

public interface ItemRepository extends JpaRepository<Item, Integer> {



    List<Item> findByMerchantId(Integer merchantId);
}