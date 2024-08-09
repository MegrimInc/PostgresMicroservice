package edu.help.microservice.repository;

import edu.help.microservice.entity.Bar;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BarRepository extends JpaRepository<Bar, Integer> {
    Optional<Bar> findByBarEmail(String bar_email);
}
