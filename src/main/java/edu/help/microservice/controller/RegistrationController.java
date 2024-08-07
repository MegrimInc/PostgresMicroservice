package edu.help.microservice.controller;

import edu.help.microservice.entity.UserData;
import edu.help.microservice.service.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/registration")
public class RegistrationController {

    @Autowired
    private RegistrationService registrationService;
    @PostMapping("/addUser")
    public UserData addUser(@RequestParam String email, @RequestParam String firstName, 
                            @RequestParam String lastName, @RequestParam boolean acceptedTOS, 
                            @RequestParam String password) {
        return registrationService.addUser(email, firstName, lastName, acceptedTOS, password);
    }

    @GetMapping("/verifyUser")
    public boolean verifyUser(@RequestParam String email, @RequestParam String password) {
        return registrationService.verifyUser(email, password);
    }
}
