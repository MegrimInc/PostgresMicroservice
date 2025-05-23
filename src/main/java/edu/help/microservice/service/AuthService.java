package edu.help.microservice.service;

import edu.help.microservice.entity.Auth;
import edu.help.microservice.repository.AuthRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private AuthRepository authRepository;

    /**
     * Finds a SignUp entity by email.
     *
     * @param email the email of the SignUp entity
     * @return the SignUp entity with the given email, or null if not found
     */
    public Auth findByEmail(String email) {
        return authRepository.findByEmail(email);
    }

    public String findEmailByMerchantId(int merchantId) {
        Optional<Auth> entity = authRepository.findByMerchant_MerchantId(merchantId);
        return entity.get().getEmail();
    }

    /**
     * Saves a SignUp entity to the database.
     *
     * @param signUp the SignUp entity to save
     * @return the saved SignUp entity
     */
    public Auth save(Auth signUp) {
        return authRepository.save(signUp);
    }

    /**
     * Finds a SignUp entity by its ID.
     *
     * @param id the ID of the SignUp entity
     * @return an Optional containing the SignUp entity if found, or empty if not
     */
    public Optional<Auth> findById(Integer id) {
        return authRepository.findById(id);
    }

    /**
     * Retrieves all SignUp entities.
     *
     * @return a list of all SignUp entities
     */
    public List<Auth> findAll() {
        return authRepository.findAll();
    }

    /**
     * Deletes a SignUp entity by its ID.
     *
     * @param id the ID of the SignUp entity to delete
     */
    public void deleteById(Integer id) {
        authRepository.deleteById(id);
    }

    /**
     * Checks if a SignUp entity exists by email.
     *
     * @param email the email to check
     * @return true if a SignUp entity with the given email exists, false otherwise
     */
    public boolean existsByEmail(String email) {
        return authRepository.findByEmail(email) != null;
    }

    public void delete(Auth signUp) {
        authRepository.delete(signUp);
    }

    /**
    * Retrieves an Auth entity associated with the given customer ID.
    *
    * @param customerId the ID of the customer linked to the Auth record
    * @return an Optional containing the Auth entity if found, or empty if not
    */
    public Optional<Auth> findByCustomerId(Integer customerId) {
        return authRepository.findByCustomer_CustomerId(customerId);
    }

}