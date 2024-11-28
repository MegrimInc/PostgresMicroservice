package edu.help.microservice.service;

import edu.help.microservice.entity.SignUp;
import edu.help.microservice.repository.SignUpRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SignUpService {

    @Autowired
    private SignUpRepository signUpRepository;

    /**
     * Finds a SignUp entity by email.
     *
     * @param email the email of the SignUp entity
     * @return the SignUp entity with the given email, or null if not found
     */
    public SignUp findByEmail(String email) {
        return signUpRepository.findByEmail(email);
    }

    public String findEmailByBarId(int barId)
    {
        Optional<SignUp> entity = signUpRepository.findByBar_BarId(barId);
        return entity.get().getEmail();
    }

    /**
     * Saves a SignUp entity to the database.
     *
     * @param signUp the SignUp entity to save
     * @return the saved SignUp entity
     */
    public SignUp save(SignUp signUp) {
        return signUpRepository.save(signUp);
    }

    /**
     * Finds a SignUp entity by its ID.
     *
     * @param id the ID of the SignUp entity
     * @return an Optional containing the SignUp entity if found, or empty if not
     */
    public Optional<SignUp> findById(Integer id) {
        return signUpRepository.findById(id);
    }

    /**
     * Retrieves all SignUp entities.
     *
     * @return a list of all SignUp entities
     */
    public List<SignUp> findAll() {
        return signUpRepository.findAll();
    }

    /**
     * Deletes a SignUp entity by its ID.
     *
     * @param id the ID of the SignUp entity to delete
     */
    public void deleteById(Integer id) {
        signUpRepository.deleteById(id);
    }

    /**
     * Checks if a SignUp entity exists by email.
     *
     * @param email the email to check
     * @return true if a SignUp entity with the given email exists, false otherwise
     */
    public boolean existsByEmail(String email) {
        return signUpRepository.findByEmail(email) != null;
    }

    public void delete(SignUp signUp) {
        signUpRepository.delete(signUp);
    }
    
}
