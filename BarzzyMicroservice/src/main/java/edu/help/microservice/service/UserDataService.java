package edu.help.microservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.*;

import edu.help.microservice.entity.UserData;
import edu.help.microservice.repository.UserDataRepository;

@Service
public class UserDataService {
    @Autowired
    private UserDataRepository userDataRepository;

    public UserData findByEmail(String email) {
        return userDataRepository.findByEmail(email).orElse(null);
    }

    public UserData save(UserData userData) {
        return userDataRepository.save(userData);
    }
}
