package edu.help.microservice.repository;

import edu.help.microservice.entity.Auth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthRepository extends JpaRepository<Auth, Integer> {

    /**
     * Finds a SignUp entity by email.
     *
     * @param email the email of the SignUp entity
     * @return the SignUp entity with the given email, or null if not found
     */
    Auth findByEmail(String email);

    // Corrected method
    Optional<Auth> findByMerchant_MerchantId(Integer merchantId);
}