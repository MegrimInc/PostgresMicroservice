package edu.help.microservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.help.microservice.entity.Drink;

import java.util.List;

public interface DrinkRepository extends JpaRepository<Drink, Integer> {
    List<Drink> findByDrinkIdIn(List<Integer> drinkIds);
    
    @Query(value = "SELECT * FROM drinks d WHERE d.bar_id = :barId ORDER BY RANDOM() LIMIT 6", nativeQuery = true)
    List<Drink> findRandom6ByBarId(@Param("barId") Integer barId);
}
