package edu.help.microservice.controller;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Properties;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.help.microservice.dto.AcceptTOSRequest;
import edu.help.microservice.dto.LoginRequest;
import edu.help.microservice.dto.VerificationRequest;
import edu.help.microservice.entity.Bar;
import edu.help.microservice.entity.Registration;
import edu.help.microservice.entity.UserData;
import edu.help.microservice.service.BarService;
import edu.help.microservice.service.RegistrationService;
import edu.help.microservice.service.UserDataService;
import jakarta.mail.internet.MimeMessage;


@RestController
@RequestMapping("/signup")
public class SignUpController {

    private static final String SECRET_KEY = "ArchistructureKnowsThatLOLITSALEXHatesPotSpam";

    private String generateHash(String email) throws NoSuchAlgorithmException {
        String text = email + SECRET_KEY;
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(text.getBytes());
        return Base64.getEncoder().encodeToString(hash);
    }

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private BarService barService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody AcceptTOSRequest request) {
        // Print incoming registration data
        System.out.println("Received registration request for email: " + request.getEmail());
    
        String email = request.getEmail();
    
        // Check if the email is already registered
        Registration existingRegistration = registrationService.findByEmail(email);
        System.out.println("Existing registration found: " + (existingRegistration != null));
    
        if (existingRegistration == null) {
            // Email does not exist, create new registration
            Registration newRegistration = new Registration();
            newRegistration.setEmail(email);
            System.out.println("Creating new registration for email: " + email);
    
            registrationService.save(newRegistration);
            String verificationCode = generateVerificationCode(); // Hardcoded for testing
            newRegistration.setPasscode(verificationCode);
            System.out.println("Generated verification code: " + verificationCode);
            
            registrationService.save(newRegistration);
            System.out.println("New registration saved for email: " + email);
    
            sendVerificationEmail(email, verificationCode); // Calling send verification email
            System.out.println("Verification email sent to: " + email);
            
            return ResponseEntity.ok("sent email");
        } else if (existingRegistration.getUserData() != null) {
            // Email exists and user data is already set
            System.out.println("Email already exists with user data: " + email);
            return ResponseEntity.status(HttpStatus.CONFLICT).body("email already exists");
        } else {
            // Email exists, but no user data
            System.out.println("Email exists, but no user data. Resending verification code.");
            String verificationCode = generateVerificationCode(); // Hardcoded for testing
            System.out.println("Generated new verification code: " + verificationCode);
    
            existingRegistration.setPasscode(verificationCode);
            registrationService.save(existingRegistration);
            System.out.println("Updated registration with new passcode for email: " + email);
    
            sendVerificationEmail(email, verificationCode); // Calling send verification email
            System.out.println("Re-sent verification email to: " + email);
            
            return ResponseEntity.ok("Re-sent email");
        }
    }

    @PostMapping("/send-verification")
    public ResponseEntity<String> sendVerification(@RequestBody AcceptTOSRequest request) {
        String email = request.getEmail();
        Registration registration = registrationService.findByEmail(email);
        //AcceptedTOS needs to be FALSE
        if (registration != null) {
            String verificationCode = generateVerificationCode(); // Hardcoded for testing
            registration.setPasscode(verificationCode);
            registrationService.save(registration);
            sendVerificationEmail(email, verificationCode); // Calling send verification email
            return ResponseEntity.ok("Verification email sent");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Email not found");
        }
    }

    @PostMapping("/deleteaccount")
public ResponseEntity<String> deleteAccount(@RequestBody LoginRequest request) {
    String email = request.getEmail();
    String password = request.getPassword();

    // Find Registration by email
    Registration registration = registrationService.findByEmail(email);

    if (registration != null) {
        // Check if it's a user account with UserData
        UserData userData = registration.getUserData();

        if (userData != null) {
            // Verify password for UserData
            if (userData.getPassword().equals(password)) {
                // Delete Registration first
                registrationService.delete(registration);
                // Then delete UserData
                userDataService.delete(userData);
                return ResponseEntity.ok("deleted");
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("incorrect password");
            }
        } else if (registration.getIsBar() != null && registration.getIsBar()) {
            // It's a bar account
            // Verify password (assuming password is stored in passcode)
            if (registration.getPasscode().equals(password)) {
                // Delete Registration
                registrationService.delete(registration);
                return ResponseEntity.ok("deleted");
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("incorrect password");
            }
        } else {
            // Registration exists but no associated UserData or Bar
            // Delete Registration
            registrationService.delete(registration);
            return ResponseEntity.ok("deleted");
        }
    } else {
        // Registration not found
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("not found");
    }
}


    @PostMapping("/verify")
    public ResponseEntity<String> verify(@RequestBody VerificationRequest verificationRequest) {
        String email = verificationRequest.getEmail();
        String verificationCode = verificationRequest.getVerificationCode();
        String password = verificationRequest.getPassword();
        String firstName = verificationRequest.getFirstName();
        String lastName = verificationRequest.getLastName();

        Registration registration = registrationService.findByEmail(email);

        if (registration != null && registration.getPasscode().equals(verificationCode)) {
            UserData newUser = new UserData();
            newUser.setEmail(email);
            newUser.setPassword(password);
            newUser.setFirstName(firstName);
            newUser.setLastName(lastName);
            newUser.setAcceptedTOS(false);
            userDataService.save(newUser);

            registration.setUserData(newUser);
            registrationService.save(registration);

            return ResponseEntity.ok(newUser.getUserID().toString());
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("REGISTRATION FAILED");
        }
    }

    @PostMapping("/accept-tos")
    public ResponseEntity<String> acceptTOS(@RequestBody AcceptTOSRequest acceptTOSRequest) {
        String email = acceptTOSRequest.getEmail();

        UserData user = userDataService.findByEmail(email);
        if (user != null) {
            user.setAcceptedTOS(true);
            userDataService.save(user);
            return ResponseEntity.ok("TOS accepted");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    }


    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest loginRequest) {
        // Print incoming request data
        System.out.println("Received login request for email: " + loginRequest.getEmail());
        System.out.println("Password provided: " + loginRequest.getPassword());

        String email = loginRequest.getEmail();
        String password = loginRequest.getPassword();

        // Retrieve data for debugging
        Registration registeredUser = registrationService.findByEmail(email);
        System.out.println("Registered user found: " + (registeredUser != null));

        UserData user = userDataService.findByEmail(email);
        System.out.println("User data found: " + (user != null));

        Bar testBar = barService.findByBarEmail(email);
        System.out.println("Bar data found: " + (testBar != null));

        // Debug the password check
        if (user != null) {
            System.out.println("Checking user password for email: " + email);
            System.out.println("User password: " + user.getPassword() + " | Entered password: " + password);
            if (user.getPassword().equals(password)) {
                System.out.println("Login successful for user: " + user.getUserID());
                return ResponseEntity.ok(user.getUserID().toString());
            } else {
                System.out.println("Password mismatch for user: " + email);
            }
        }

        // Debug passcode check for bar users
        if (registeredUser != null) {
            System.out.println("Checking passcode for registered user email: " + email);
            System.out.println(
                    "Registered passcode: " + registeredUser.getPasscode() + " | Entered passcode: " + password);
            System.out.println("Is bar account: " + registeredUser.getIsBar());
            if (registeredUser.getPasscode().equals(password) && registeredUser.getIsBar()) {
                System.out.println("Login successful for bar: " + testBar.getBarId());
                return ResponseEntity.ok("" + (testBar.getBarId() * -1));
            } else {
                System.out.println("Passcode mismatch or user is not a bar: " + email);
            }
        }

        // Print failure message if login did not succeed
        System.out.println("Login failed for email: " + email);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("LOGIN FAILED");
    }




    private String generateVerificationCode() {
        // Generate a random verification code
        Random rand = new Random();

        return "" + rand.nextInt(0, 9) +
                rand.nextInt(0, 9) +
                rand.nextInt(0, 9) +
                rand.nextInt(0, 9) +
                rand.nextInt(0, 9) +
                rand.nextInt(0, 9);
    }

    private void sendVerificationEmail(String email, String code) {
        JavaMailSenderImpl test = new JavaMailSenderImpl();


        test.setHost("email-smtp.us-east-1.amazonaws.com");
        test.setPort(587);

        test.setUsername("AKIARKMXJUVKGK3ZC6FH");
        test.setPassword("BJ0EwGiCXsXWcZT2QSI5eR+5yFzbimTnquszEXPaEXsd");

        Properties props = test.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true");

        try {
            // Create a MimeMessage
            MimeMessage message = test.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            // Set the basic email attributes
            helper.setTo(email);
            helper.setFrom("noreply@barzzy.site");
            helper.setSubject("Barzzy Verification Code");

            // Generate hash for the email
            String unsubscribeHash = generateHash(email);
            String unsubscribeUrl = "https://www.barzzy.site/signup/unsubscribe?email=" + email + "&hash=" + unsubscribeHash;

            // HTML content with clickable link
            String htmlContent = "<p>Your verification code is: <strong>" + code + "</strong>.</p>"
                    + "<p>If you did not wish to receive this email, click here to <a href='" + unsubscribeUrl + "'>unsubscribe from all emails</a>.</p>";

            // Set the email content as HTML
            helper.setText(htmlContent, true);  // Set 'true' to indicate HTML content

            // Send the email
            test.send(message);
            System.out.println("Successful send");
        }
        catch(Exception ex) {
            // Log any exceptions
            System.err.println(ex.getMessage());
        }



    }

    @GetMapping("/unsubscribe")
    public ResponseEntity<String> unsubscribe(@RequestParam("email") String email,
                                              @RequestParam("hash") String hash) {
System.out.println("Email: " + email + ", Hash: " + hash);
        try {
            String expectedHash = generateHash(email);
            if (expectedHash.equals(hash)) {
                // Process the unsubscribe request (not implemented here)
                return ResponseEntity.ok("Successfully unsubscribed " + email);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid unsubscribe link");
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing request");
        }
    }

        // Helper method to validate email format
        private boolean isValidEmail(String email) {
            String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
            return email.matches(emailRegex);
        }
}




