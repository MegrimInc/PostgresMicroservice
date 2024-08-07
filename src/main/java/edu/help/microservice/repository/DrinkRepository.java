package edu.help.microservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.help.microservice.entity.Drink;

import java.util.List;

public interface DrinkRepository extends JpaRepository<Drink, Integer> {
    List<Drink> findByDrinkIdIn(List<Integer> drinkIds);
}
