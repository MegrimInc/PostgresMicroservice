package edu.help.microservice.controller;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Properties;
import java.util.Random;

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

import com.stripe.exception.StripeException;

import edu.help.microservice.dto.AcceptTOSRequest;
import edu.help.microservice.dto.AcceptTOSRequest2;
import edu.help.microservice.dto.LoginRequest;
import edu.help.microservice.dto.ResetPasswordConfirmRequest;
import edu.help.microservice.dto.VerificationBarRequest;
import edu.help.microservice.dto.VerificationRequest;
import edu.help.microservice.dto.VerifyResetCodeRequest;
import edu.help.microservice.entity.Activity;
import edu.help.microservice.entity.Bar;
import edu.help.microservice.entity.Customer;
import edu.help.microservice.entity.SignUp;
import edu.help.microservice.service.ActivityService;
import edu.help.microservice.service.BarService;       // For "this hour"
import edu.help.microservice.service.CustomerService; // If needed for logging
import edu.help.microservice.service.SignUpService;
import edu.help.microservice.service.StripeService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/newsignup")
public class NewSignUpController {
    private static final String SECRET_KEY = "YourSecretKey";

    private final SignUpService signUpService;
    private final CustomerService customerService;
    private final BarService barService;
    private final StripeService stripeService;
    private final ActivityService activityService;

    /**
     * The pay-to-use "heartbeat" call from the frontend.
     *  1) if startDate == null, set it to now -> stop
     *  2) if startDate < 30 days old, do nothing -> stop
     *  3) otherwise, record usage for this hour (placeholder).
     */
    @PostMapping("/heartbeat")
    public ResponseEntity<String> heartbeat(@RequestParam("barId") String barID,
                                            @RequestParam("bartenderId") String bartenderID) {
        System.out.println("heartbeat initiated for bar " + barID);
        try {
            int barIdInt = Integer.parseInt(barID);
            Bar bar = barService.findBarById(barIdInt);
            if (bar == null) {
                System.out.println("heartbeat: no bar found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No bar found with ID " + barID);
            }

            System.out.println("heartbeat: Bar Found");

            // 1) If bar.startDate is null, set it and stop
            if (bar.getStartDate() == null) {

                System.out.println("heartbeat: attempting startdate setting.");
                barService.setStartDate(barIdInt, LocalDate.now());
                System.out.println("heartbeat: Started free trial");
                return ResponseEntity.ok("startDate was null, now set to today. Done.");
            }

            // 2) Check how long ago startDate was
            LocalDate startDate = bar.getStartDate();
            long daysSinceStart = ChronoUnit.DAYS.between(startDate, LocalDate.now());
            if (daysSinceStart < 30) {
                // Still within free trial
                System.out.println("heartbeat: Within free trial");
                return ResponseEntity.ok("Within 30-day free trial. Done.");
            }

            // 3) In paid territory
            LocalDateTime currentHour = LocalDateTime.now()
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);

            // Check if we have an entry for (barIdInt, bartenderID, currentHour)
            if (!activityService.alreadyRecordedThisHour(barIdInt, bartenderID, currentHour)) {
                Activity a1 = activityService.recordActivity(barIdInt, bartenderID, currentHour);
                String debugMessage = String.format("Recorded usage for bar %d, bartender %s at hour %s",
                        barIdInt, bartenderID, currentHour.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                if (a1.getActivityId() != null) {
                    System.out.println("heartbeat: Recorded: " + debugMessage);

                    System.out.println("Attempting stripe charge");
                    stripeService.sendMeterEvent(bar);
                    System.out.println("Stripe charge probably successful");
                }
                return ResponseEntity.ok(debugMessage);
            } else {
                System.out.println("heartbeat: Within Hour");
                return ResponseEntity.ok("Usage for this hour was already recorded. Nothing to do.");
            }
        } catch (Exception e) {
            System.out.println("heartbeat: failed with stacktrace: " + e.getStackTrace() + " and also this: " + e.getCause() + " anddddd this... : " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("heartbeat: failed with stacktrace: " + e.getStackTrace() + " and also this: " + e.getCause() + " anddddd this... : " + e.getMessage());
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
                signUp.setVerificationCode(hash(verificationCode));
            } catch (NoSuchAlgorithmException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("error generating verification code");
            }
            signUp.setExpiryTimestamp(generateExpiryTimestamp());
            signUpService.save(signUp);
            sendVerificationEmail(email, verificationCode, "Registration");
            return ResponseEntity.ok("verification email sent");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("email not found");
        }
    }

    // ENDPOINT #3: Verification for Customer
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
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("registration failed: verification code expired");
            }
            try {
                String hashedCode = hash(verificationCode);
                if (signUp.getVerificationCode().equals(hashedCode)) {
                    // Verification successful
                    Customer customer = new Customer();
                    customer.setFirstName(firstName);
                    customer.setLastName(lastName);

                    // Hash the password and set passcode
                    String hashedPassword = hash(password);
                    signUp.setPasscode(hashedPassword);

                    // Clear the verification code and expiry timestamp
                    signUp.setVerificationCode(null);
                    signUp.setExpiryTimestamp(null);

                    customerService.save(customer); // Save customer

                    signUp.setCustomer(customer);
                    signUpService.save(signUp); // Save sign-up details with linked customer

                    // Return customerID
                    return ResponseEntity.ok(customer.getCustomerID().toString());
                } else {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body("registration failed: incorrect verification code");
                }
            } catch (NoSuchAlgorithmException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error processing verification");
            }
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("registration failed");
        }
    }

    @PostMapping("/register2")
    public ResponseEntity<String> register(@RequestBody AcceptTOSRequest2 request) {
        String email = request.getEmail();
        SignUp existingSignUp = signUpService.findByEmail(email);

        if (existingSignUp != null)
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already exists");

        // Create new sign-up and customer
        SignUp newSignUp = new SignUp();
        newSignUp.setEmail(email);
        newSignUp.setIsBar(false);

        Customer customer = new Customer();
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());

        // Hash and store the password immediately
        try {
            String hashedPassword = hash(request.getPassword());
            newSignUp.setPasscode(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error hashing password");
        }

        // Create Stripe customer and save the customer ID
        try {
            stripeService.createStripeCustomer(customer, newSignUp);
        } catch (StripeException e) {
            System.out.println("Error creating Stripe customer:");
            System.out.println("    Message: " + e.getMessage());
            System.out.println("    Status Code: " + e.getStatusCode());
            System.out.println("    Type: " + e.getCause());
            System.out.println("    Request ID: " + e.getRequestId());
            e.printStackTrace();  // Prints the full stack trace for further diagnosis
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating Stripe customer");
        }

        // Save customer and link with sign-up
        customerService.save(customer);
        newSignUp.setCustomer(customer);
        signUpService.save(newSignUp);

        // Return the customer ID as a string
        return ResponseEntity.ok(String.valueOf(customer.getCustomerID()));
    }

    // ENDPOINT: Verify Bar Registration
    @PostMapping("/verify/bar")
    public ResponseEntity<String> verifyBar(@RequestBody VerificationBarRequest verificationRequest) {
        String email = verificationRequest.getEmail();
        String verificationCode = verificationRequest.getVerificationCode();
        String password = verificationRequest.getPassword();

        SignUp signUp = signUpService.findByEmail(email);

        if (signUp != null && signUp.getBar() == null && signUp.getIsBar()) {
            if (isVerificationCodeExpired(signUp.getExpiryTimestamp())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("registration failed: verification code expired");
            }
            try {
                String hashedCode = hash(verificationCode);
                if (signUp.getVerificationCode().equals(hashedCode)) {
                    // Verification successful
                    Bar bar = new Bar();
                    bar.setBarEmail(email);
                    bar.setBarName(verificationRequest.getCompanyName());
                    bar.setBarTag(verificationRequest.getCompanyNickname());
                    bar.setBarCountry(verificationRequest.getCountry());
                    bar.setBarState(verificationRequest.getRegion());
                    bar.setBarCity(verificationRequest.getCity());
                    bar.setBarAddress(verificationRequest.getAddress());
                    bar.setTagImage(""); // Set default or handle accordingly
                    bar.setBarImage(""); // Set default or handle accordingly
                    bar.setOpenHours(verificationRequest.getOpenTime() + " - " + verificationRequest.getCloseTime());

                    // Hash the password and set passcode
                    String hashedPassword = hash(password);
                    signUp.setPasscode(hashedPassword);

                    // Clear the verification code and expiry timestamp
                    signUp.setVerificationCode(null);
                    signUp.setExpiryTimestamp(null);

                    barService.save(bar); // Save the Bar entity

                    signUp.setBar(bar); // Associate the Bar with SignUp
                    signUpService.save(signUp); // Save SignUp with the associated Bar

                    // Return negative Bar ID to differentiate from customer IDs
                    return ResponseEntity.ok("-" + bar.getBarId().toString());
                } else {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body("registration failed: incorrect verification code");
                }
            } catch (NoSuchAlgorithmException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error processing verification");
            }
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("registration failed");
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
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("user not found");
        }
    }

    // ENDPOINT #5: Login with email and password
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest loginRequest) {
        String email = loginRequest.getEmail();
        String password = loginRequest.getPassword();

        SignUp signUp = signUpService.findByEmail(email);

        if (signUp != null) {
            try {
                String hashedPassword = hash(password);
                if (signUp.getPasscode() != null && signUp.getPasscode().equals(hashedPassword)) {
                    if (signUp.getBar() != null) {
                        // Bar login
                        return ResponseEntity.ok("" + signUp.getBar().getBarId() * -1);
                    } else if (signUp.getCustomer() != null) {
                        // Customer login
                        return ResponseEntity.ok(signUp.getCustomer().getCustomerID().toString());
                    } else {
                        // No associated customer or bar
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("login failed");
                    }
                } else {
                    // Password mismatch
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("login failed");
                }
            } catch (NoSuchAlgorithmException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error processing login");
            }
        } else {
            // SignUp not found
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("login failed");
        }
    }

    // ENDPOINT: Delete Account
    @PostMapping("/deleteaccount")
    public ResponseEntity<String> deleteAccount(@RequestBody LoginRequest loginRequest) {
        String email = loginRequest.getEmail();
        String password = loginRequest.getPassword();
        SignUp signUp = signUpService.findByEmail(email);

        if (signUp != null) {
            try {
                String hashedPassword = hash(password);

                // Verify the password
                if (signUp.getPasscode() != null && signUp.getPasscode().equals(hashedPassword)) {
                    // Check if it's a customer account
                    if (signUp.getCustomer() != null) {
                        Customer customer = signUp.getCustomer();
                        customerService.delete(customer); // Delete the customer entity
                        signUpService.delete(signUp); // Delete the sign-up record
                        return ResponseEntity.ok("customer account deleted");
                    }
                    // Check if it's a bar account
                    else if (signUp.getBar() != null) {
                        Bar bar = signUp.getBar();
                        barService.delete(bar); // Delete the bar entity
                        signUpService.delete(signUp); // Delete the sign-up record
                        return ResponseEntity.ok("bar account deleted");
                    }
                    // If no customer or bar is associated
                    else {
                        signUpService.delete(signUp);
                        return ResponseEntity.ok("account deleted");
                    }
                } else {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("incorrect password");
                }
            } catch (NoSuchAlgorithmException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error processing request");
            }
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("account not found");
        }
    }

    // Helper methods
    private String generateVerificationCode() {
        // Generate a 6-digit random verification code
        Random rand = new Random();
        return String.format("%06d", rand.nextInt(999999));
    }

    private void sendVerificationEmail(String email, String code, String title) {
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
            helper.setSubject("Barzzy Verification Code - " + title);

            // Generate hash for the email
            String unsubscribeHash = generateHash(email);
            String unsubscribeUrl = "https://www.barzzy.site/signup/unsubscribe?email=" + email + "&hash="
                    + unsubscribeHash;

            // HTML content with clickable link
            String htmlContent = "<p>Your verification code is: <strong>" + code + "</strong>.</p>"
                    + "<p>If you did not wish to receive this email, click here to <a href='" + unsubscribeUrl
                    + "'>unsubscribe from all emails</a>.</p>";

            // Set the email content as HTML
            helper.setText(htmlContent, true); // Set 'true' to indicate HTML content

            // Send the email
            test.send(message);
            System.out.println("Successful send");
        } catch (Exception ex) {
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

    // ... existing code ...

    // Endpoint 1: Check if an account exists and send a verification code
    @PostMapping("/reset-password-validate-email")
    public ResponseEntity<String> resetPasswordValidateEmail(@RequestBody AcceptTOSRequest request) {
        String email = request.getEmail();
        SignUp signUp = signUpService.findByEmail(email);

        if (signUp != null) {
            // Generate a new verification code
            String verificationCode = generateVerificationCode();
            try {
                signUp.setVerificationCode(hash(verificationCode));
            } catch (NoSuchAlgorithmException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("error generating verification code");
            }
            signUp.setExpiryTimestamp(generateExpiryTimestamp());
            signUpService.save(signUp);
            // Send the verification code via email
            sendVerificationEmail(email, verificationCode, "Forgot Password");
            return ResponseEntity.ok("verification code sent");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("email not found");
        }
    }

    // Endpoint 2: Verify the verification code
    @PostMapping("/reset-password-validate-code")
    public ResponseEntity<String> resetPasswordVerifyCode(@RequestBody VerifyResetCodeRequest request) {
        String email = request.getEmail();
        String verificationCode = request.getCode();

        SignUp signUp = signUpService.findByEmail(email);

        if (signUp != null) {
            if (isVerificationCodeExpired(signUp.getExpiryTimestamp())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("verification code expired");
            }
            try {
                String hashedCode = hash(verificationCode);
                if (hashedCode.equals(signUp.getVerificationCode())) {
                    // Verification code is correct
                    return ResponseEntity.ok("verification code valid");
                } else {
                    // Incorrect verification code
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("incorrect verification code");
                }
            } catch (NoSuchAlgorithmException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error processing request");
            }
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("email not found");
        }
    }

    // Endpoint 3: Reset the password
    @PostMapping("/reset-password-final")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordConfirmRequest request) {
        String email = request.getEmail();
        String verificationCode = request.getCode();
        String newPassword = request.getPassword();

        SignUp signUp = signUpService.findByEmail(email);

        if (signUp != null) {
            if (isVerificationCodeExpired(signUp.getExpiryTimestamp())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("verification code expired");
            }
            try {
                String hashedCode = hash(verificationCode);
                if (hashedCode.equals(signUp.getVerificationCode())) {
                    // Verification code is correct
                    // Hash the new password and update the passcode
                    String hashedPassword = hash(newPassword);
                    signUp.setPasscode(hashedPassword);
                    // Clear the verification code and expiry timestamp
                    signUp.setVerificationCode(null);
                    signUp.setExpiryTimestamp(null);
                    signUpService.save(signUp);
                    return ResponseEntity.ok("password reset successful");
                } else {
                    // Incorrect verification code
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("incorrect verification code");
                }
            } catch (NoSuchAlgorithmException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error processing request");
            }
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("email not found");
        }
    }

    private String generateHash(String email) throws NoSuchAlgorithmException {
        String text = email + SECRET_KEY;
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = md.digest(text.getBytes());
        return Base64.getEncoder().encodeToString(hashBytes);
    }

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
        return expiryTimestamp != null && expiryTimestamp.before(new Timestamp(System.currentTimeMillis()));
    }
}
