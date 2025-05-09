package edu.help.microservice.controller;

import edu.help.microservice.entity.Config;
import edu.help.microservice.repository.ConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/postgres-test-api/config")
public class ConfigController {

    private final ConfigRepository configRepository;

    @Autowired
    public ConfigController(ConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    @GetMapping
    public ResponseEntity<?> getAllConfig() {
        try {
            List<Config> configs = configRepository.findAll();
            Map<String, Object> response = new HashMap<>();
            for (Config config : configs) {
                response.put(config.getKey(), config.getValue());
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error fetching config data");
        }
    }
}