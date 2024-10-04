package edu.help.microservice.repository;

import edu.help.microservice.entity.Registration;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationRepository extends JpaRepository<Registration, Integer> {
    Optional<Registration> findByEmail(String email);
}
