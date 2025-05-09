package edu.help.microservice.repository;

import edu.help.microservice.entity.Config;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfigRepository extends JpaRepository<Config, Integer> {
    Config findByKey(String key);
}