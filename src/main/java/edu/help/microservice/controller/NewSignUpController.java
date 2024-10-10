package edu.help.microservice.controller;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
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
import edu.help.microservice.entity.Customer;
import edu.help.microservice.entity.SignUp;
import edu.help.microservice.service.CustomerService;
import edu.help.microservice.service.SignUpService;
import jakarta.mail.internet.MimeMessage;

@RestController
@RequestMapping("/newsignup")
public class NewSignUpController {

    private static final String SECRET_KEY = "YourSecretKey";

    private String hash(String input) throws NoSuchAlgorithmException {
        String text = input + SECRET_KEY;
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(text.getBytes());
        return Base64.getEncoder().encodeToString(hash);
    }

    private Timestamp generateExpiryTimestamp() {
        long expiryTime = System.currentTimeMillis() + 15 * 60 * 1000; // 15 minutes from now
        return new Timestamp(expiryTime);
    }

    private boolean isVerificationCodeExpired(Timestamp expiryTimestamp) {
        return expiryTimestamp.before(new Timestamp(System.currentTimeMillis()));
    }

    @Autowired
    private SignUpService signUpService;

    @Autowired
    private CustomerService customerService;

    // ENDPOINT #1: Register a new user
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody AcceptTOSRequest request) {
        String email = request.getEmail();

        SignUp existingSignUp = signUpService.findByEmail(email);

        if (existingSignUp == null) {
            // Email does not exist
            SignUp newSignUp = new SignUp();
            newSignUp.setEmail(email);
            String verificationCode = generateVerificationCode();
            try {
                newSignUp.setPasscode(hash(verificationCode));
            } catch (NoSuchAlgorithmException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            newSignUp.setExpiryTimestamp(generateExpiryTimestamp());
            signUpService.save(newSignUp);
            //Saved Signup

            sendVerificationEmail(email, verificationCode);
            return ResponseEntity.ok("sent email");
        } else if (existingSignUp.getCustomer() != null) {
            // Email exists and customer exists
            return ResponseEntity.status(HttpStatus.CONFLICT).body("email already exists");
        } else {
            // Email exists but customer does not exist (verification not completed)
            String verificationCode = generateVerificationCode();
            try {
                existingSignUp.setPasscode(hash(verificationCode));
            } catch (NoSuchAlgorithmException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error generating verification code");
            }
            existingSignUp.setExpiryTimestamp(generateExpiryTimestamp());
            signUpService.save(existingSignUp);
            sendVerificationEmail(email, verificationCode);
            return ResponseEntity.ok("Re-sent email");
        }
    }

    // ENDPOINT #2: Resend verification code
    @PostMapping("/send-verification")
    public ResponseEntity<String> sendVerification(@RequestBody AcceptTOSRequest request) {
        String email = request.getEmail();
        SignUp signUp = signUpService.findByEmail(email);

        if (signUp != null) {
            String verificationCode = generateVerificationCode();
            try {
                signUp.setPasscode(hash(verificationCode));
            } catch (NoSuchAlgorithmException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error generating verification code");
            }
            signUp.setExpiryTimestamp(generateExpiryTimestamp());
            signUpService.save(signUp);
            sendVerificationEmail(email, verificationCode);
            return ResponseEntity.ok("Verification email sent");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Email not found");
        }
    }

//ENDPOINT #3: VERIFICATION
    @PostMapping("/verify")
public ResponseEntity<String> verify(@RequestBody VerificationRequest verificationRequest) {
    String email = verificationRequest.getEmail();
    String verificationCode = verificationRequest.getVerificationCode();
    String password = verificationRequest.getPassword();
    String firstName = verificationRequest.getFirstName();
    String lastName = verificationRequest.getLastName();

    SignUp signUp = signUpService.findByEmail(email);

    if (signUp != null && signUp.getCustomer() == null) {
        if (isVerificationCodeExpired(signUp.getExpiryTimestamp())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("REGISTRATION FAILED");
        }
        try {
            String hashedCode = hash(verificationCode);
            if (signUp.getPasscode().equals(hashedCode)) {
                // Verification successful
                Customer customer = new Customer();
                customer.setFirstName(firstName);
                customer.setLastName(lastName);
                customerService.save(customer);  // Save customer

                signUp.setCustomer(customer);
                signUpService.save(signUp);  // Save sign-up details with linked customer

                // Return customerID (mapped to userID in old code)
                return ResponseEntity.ok(customer.getCustomerID().toString());
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("REGISTRATION FAILED");
            }
        } catch (NoSuchAlgorithmException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing verification");
        }
    } else {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("REGISTRATION FAILED");
    }
}


    // ENDPOINT #4: Accept terms of service
    @PostMapping("/accept-tos")
    public ResponseEntity<String> acceptTOS(@RequestBody AcceptTOSRequest request) {
        String email = request.getEmail();
        SignUp signUp = signUpService.findByEmail(email);

        if (signUp != null && signUp.getCustomer() != null) {
            Customer customer = signUp.getCustomer();
            customer.setAcceptedTOS(true);
            customerService.save(customer);
            return ResponseEntity.ok("TOS accepted");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    }

    // ENDPOINT #5: Login with email and password
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest loginRequest) {
        String email = loginRequest.getEmail();
        String password = loginRequest.getPassword();

        SignUp signUp = signUpService.findByEmail(email);

        if (signUp != null && signUp.getCustomer() != null) {
            try {
                String hashedPassword = hash(password);
                if (signUp.getPasscode().equals(hashedPassword)) {
                    return ResponseEntity.ok("LOGIN SUCCEEDED");
                } else {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("LOGIN FAILED");
                }
            } catch (NoSuchAlgorithmException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing login");
            }
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("LOGIN FAILED");
        }
    }

    // Helper methods
    private String generateVerificationCode() {
        // Generate a 6-digit random verification code
        Random rand = new Random();
        return String.format("%06d", rand.nextInt(999999));
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

    private String generateHash(String email) throws NoSuchAlgorithmException {
        String text = email + SECRET_KEY;
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = md.digest(text.getBytes());
        return Base64.getEncoder().encodeToString(hashBytes);
    }
}
