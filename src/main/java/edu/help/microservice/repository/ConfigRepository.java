package edu.help.microservice.repository;

import edu.help.microservice.entity.Config;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ConfigRepository extends JpaRepository<Config, String> {
    Optional<Config> findByKey(String key);
}