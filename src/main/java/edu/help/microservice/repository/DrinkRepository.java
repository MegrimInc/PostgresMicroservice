package edu.help.microservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.help.microservice.entity.Drink;

public interface DrinkRepository extends JpaRepository<Drink, Integer> {

    List<Drink> findByDrinkIdIn(List<Integer> drinkIds);

    // Query to find all drinks by barId, excluding specific fields
    @Query("SELECT new edu.help.microservice.entity.Drink(d.drinkId, d.barId, d.drinkName, d.alcoholContent, d.drinkImage, d.drinkTags, d.description, d.point, d.singlePrice, d.singleHappyPrice, d.doublePrice, d.doubleHappyPrice) " +
       "FROM Drink d WHERE d.barId = :barId")
    List<Drink> findAllDrinksByBarIdExcludingFields(@Param("barId") Integer barId);
}