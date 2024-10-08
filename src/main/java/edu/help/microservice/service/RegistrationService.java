package edu.help.microservice.service;

import edu.help.microservice.entity.Registration;
import edu.help.microservice.repository.RegistrationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RegistrationService {
    @Autowired
    private RegistrationRepository registrationRepository;

    public Registration findByEmail(String email) {
        return registrationRepository.findByEmail(email).orElse(null);
    }

    public Registration save(Registration registration) {
        return registrationRepository.save(registration);
    }

    public void delete(Registration registration) {
        registrationRepository.delete(registration);
    }
}

