package edu.help.microservice.service;

import edu.help.microservice.entity.Registration;
import edu.help.microservice.entity.UserData;
import edu.help.microservice.repository.RegistrationRepository;
import edu.help.microservice.repository.UserDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationService.class);

    @Autowired
    private UserDataRepository userDataRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Transactional
    public UserData addUser(String email, String firstName, String lastName, boolean acceptedTOS, String password) {
        // Check if the email already exists in UserData
        if (userDataRepository.findByEmail(email) != null) {
            logger.error("Email already exists: " + email);
            throw new IllegalArgumentException("Email already exists: " + email);
        }

        // Log user creation start
        logger.info("Creating new user with email: " + email);

        // Create and save UserData
        UserData userData = new UserData();
        userData.setEmail(email);
        userData.setFirstName(firstName);
        userData.setLastName(lastName);
        userData.setAcceptedTOS(acceptedTOS);
        userData.setPassword(password); // Ensure passwords are hashed in practice

        try {
            // Save UserData and get generated ID
            UserData savedUserData = userDataRepository.save(userData);
            Long userId = savedUserData.getUserId();

            // Logging for debugging
            logger.info("UserData saved with UserID: " + userId);

            // Create and save Registration
            Registration registration = new Registration();
            registration.setEmail(email);
            registration.setPassword(password); // Ensure passwords are hashed in practice
            registration.setUserId(userId); // Correct userId usage
            registration.setBarId(null);
            registrationRepository.save(registration);

            logger.info("Registration saved for user with email: " + email);

            return savedUserData;
        } catch (Exception e) {
            logger.error("Error occurred during user registration: ", e);
            throw e; // Rethrow the exception to trigger transaction rollback
        }
    }

    public boolean verifyUser(String email, String password) {
        // Fetch the registration entry based on email and password
        Registration registration = registrationRepository.findByEmailAndPassword(email, password);

        // Return true if a registration entry exists; otherwise, return false
        return registration != null;
    }
}
