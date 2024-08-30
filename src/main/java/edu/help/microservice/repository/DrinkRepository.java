package edu.help.microservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.help.microservice.entity.Drink;

public interface DrinkRepository extends JpaRepository<Drink, Integer> {
    
    List<Drink> findByDrinkIdIn(List<Integer> drinkIds);


   // Query to find 6 random drink IDs for a specific bar and category
   @Query(value = "SELECT drink_id FROM bar_inventory WHERE bar_id = :barId AND category_id = :categoryId ORDER BY RANDOM() LIMIT 6", nativeQuery = true)
   List<Integer> findRandom6DrinkIdsByCategoryAndBar(@Param("barId") Integer barId, @Param("categoryId") Integer categoryId);
   
}
