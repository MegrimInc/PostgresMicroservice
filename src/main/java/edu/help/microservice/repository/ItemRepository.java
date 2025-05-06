package edu.help.microservice.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import edu.help.microservice.entity.Item;

public interface ItemRepository extends JpaRepository<Item, Integer> {



    List<Item> findByMerchantId(Integer merchantId);
}