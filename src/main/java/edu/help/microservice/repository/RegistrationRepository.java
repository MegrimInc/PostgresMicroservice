package edu.help.microservice.repository;

import edu.help.microservice.entity.Registration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    Registration findByEmailAndPassword(String email, String password);
}
