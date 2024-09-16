package edu.help.microservice.controller;

import java.util.Properties;
import java.util.Random;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.*;

import edu.help.microservice.dto.AcceptTOSRequest;
import edu.help.microservice.dto.LoginRequest;
import edu.help.microservice.dto.VerificationRequest;
import edu.help.microservice.entity.Bar;
import edu.help.microservice.entity.Registration;
import edu.help.microservice.entity.UserData;
import edu.help.microservice.service.BarService;
import edu.help.microservice.service.RegistrationService;
import edu.help.microservice.service.UserDataService;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;


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
        String email = request.getEmail();

        Registration existingRegistration = registrationService.findByEmail(email);

        if (existingRegistration == null) {
            // Email does not exist
            Registration newRegistration = new Registration();
            newRegistration.setEmail(email);
            registrationService.save(newRegistration);
            String verificationCode = generateVerificationCode(); // Hardcoded for testing
            newRegistration.setPasscode(verificationCode);
            registrationService.save(newRegistration);
            sendVerificationEmail(email, verificationCode); // Calling send verification email
            return ResponseEntity.ok("sent email");
        } else if (existingRegistration.getUserData() != null) {
            // Email exists and first name exists
            return ResponseEntity.status(HttpStatus.CONFLICT).body("email already exists");
        } else {
            // Email exists and first name does not exist
            String verificationCode = generateVerificationCode(); // Hardcoded for testing
            existingRegistration.setPasscode(verificationCode);
            registrationService.save(existingRegistration);
            sendVerificationEmail(email, verificationCode);  // Calling send verification email
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
        String email = loginRequest.getEmail();
        String password = loginRequest.getPassword();
//ACCEPT TOS HAS TO BE TRUE
        Registration registeredUser = registrationService.findByEmail(email);
        UserData user = userDataService.findByEmail(email);
        Bar testBar = barService.findByBarEmail(email);

        if (user != null && user.getPassword().equals(password)) {
            return ResponseEntity.ok(user.getUserID().toString());
        } else if (registeredUser != null && registeredUser.getPasscode().equals(password) && registeredUser.getIsBar()) {
            return ResponseEntity.ok("" + (testBar.getBarId()*-1));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("LOGIN FAILED");
        }
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




