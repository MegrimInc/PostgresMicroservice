package edu.help.microservice.repository;

import edu.help.microservice.entity.SignUp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SignUpRepository extends JpaRepository<SignUp, Integer> {

    /**
     * Finds a SignUp entity by email.
     *
     * @param email the email of the SignUp entity
     * @return the SignUp entity with the given email, or null if not found
     */
    SignUp findByEmail(String email);

    Optional<SignUp> findByBar_Id(Integer barId);
}
