package edu.help.microservice.controller;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
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
import edu.help.microservice.dto.MerchantRegistrationRequest;
import edu.help.microservice.dto.ResetPasswordConfirmRequest;
import edu.help.microservice.dto.VerificationMerchantRequest;       // For "this hour"
import edu.help.microservice.dto.VerificationCustomerRequest; // If needed for logging
import edu.help.microservice.dto.VerifyResetCodeRequest;
import edu.help.microservice.entity.Merchant;
import edu.help.microservice.entity.Customer;
import edu.help.microservice.entity.SignUp;
import edu.help.microservice.service.MerchantService;
import edu.help.microservice.service.CustomerService;
import edu.help.microservice.service.SignUpService;
import edu.help.microservice.service.StripeService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
@RestController
@RequestMapping("/signup")
public class SignUpController {
    private static final String SECRET_KEY = "YourSecretKey";

    private final SignUpService signUpService;
    private final CustomerService customerService;
    private final MerchantService merchantService;
    private final StripeService stripeService;



    // ENDPOINT: Resend verification code
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


    // ENDPOINT: Verification for Registration
    @PostMapping("/verify/customer")
    public ResponseEntity<String> verifyCustomer(@RequestBody VerificationCustomerRequest verificationRequest) {
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
                    return ResponseEntity.ok(customer.getCustomerId().toString());
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


    // ENDPOINT: Registration for Customers
    @PostMapping("/register/customer")
    public ResponseEntity<String> registerCustomer(@RequestBody AcceptTOSRequest2 request) {
        String email = request.getEmail();
        SignUp existingSignUp = signUpService.findByEmail(email);

        if (existingSignUp != null)
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already exists");

        // Create new sign-up and customer
        SignUp newSignUp = new SignUp();
        newSignUp.setEmail(email);
        newSignUp.setIsMerchant(false);

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
            e.printStackTrace(); // Prints the full stack trace for further diagnosis
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating Stripe customer");
        }

        // Save customer and link with sign-up
        customerService.save(customer);
        newSignUp.setCustomer(customer);
        signUpService.save(newSignUp);

        // Return the customer ID as a string
        return ResponseEntity.ok(String.valueOf(customer.getCustomerId()));
    }
    
     
    // ENDPOINT: Verify Merchant Registration
    @PostMapping("/verify/merchant")
    public ResponseEntity<String> verifyMerchant(@RequestBody VerificationMerchantRequest verificationRequest) {
        String email = verificationRequest.getEmail();
        String verificationCode = verificationRequest.getVerificationCode();
        String password = verificationRequest.getPassword();

        SignUp signUp = signUpService.findByEmail(email);

        if (signUp != null && signUp.getMerchant() == null && signUp.getIsMerchant()) {
            if (isVerificationCodeExpired(signUp.getExpiryTimestamp())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("registration failed: verification code expired");
            }
            try {
                String hashedCode = hash(verificationCode);
                if (signUp.getVerificationCode().equals(hashedCode)) {
                    // Verification successful
                    Merchant merchant = new Merchant();
                    merchant.setName(verificationRequest.getCompanyName());
                    merchant.setNickname(verificationRequest.getCompanyNickname());
                    merchant.setCountry(verificationRequest.getCountry());
                    merchant.setStateOrProvince(verificationRequest.getStateOrProvince());
                    merchant.setCity(verificationRequest.getCity());
                    merchant.setAddress(verificationRequest.getAddress());
                    merchant.setLogoImage(""); // Set default or handle accordingly
                    merchant.setStoreImage(""); // Set default or handle accordingly
                    merchant.setOpen(false);
                    // Hash the password and set passcode
                    String hashedPassword = hash(password);
                    signUp.setPasscode(hashedPassword);

                    // Clear the verification code and expiry timestamp
                    signUp.setVerificationCode(null);
                    signUp.setExpiryTimestamp(null);

                    merchantService.save(merchant); // Save the Merchant entity

                    signUp.setMerchant(merchant); // Associate the Merchant with SignUp
                    signUpService.save(signUp); // Save SignUp with the associated Merchant

                    // Return negative Merchant ID to differentiate from customer IDs
                    return ResponseEntity.ok("-" + merchant.getId().toString());
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

   // ENDPOINT: Verify Merchant Registration
@PostMapping("/register/merchant")
public ResponseEntity<String> registerMerchant(@RequestBody MerchantRegistrationRequest req) {
    // 1) Check if there's already a SignUp record with this email
    SignUp existingSignUp = signUpService.findByEmail(req.getEmail());
    if (existingSignUp != null) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                             .body("Email already in use");
    }


    // 2) Create the SignUp record
    SignUp newSignUp = new SignUp();
    newSignUp.setEmail(req.getEmail());
    newSignUp.setIsMerchant(true);
   
    // Hash and store the password
    try {
        String hashedPassword = hash(req.getPassword());
        newSignUp.setPasscode(hashedPassword);
    } catch (NoSuchAlgorithmException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .body("Error hashing password");
    }


    // 3) Create the Merchant entity
    Merchant merchant = new Merchant();
    merchant.setName(req.getName());
    merchant.setNickname(req.getNickname());
    merchant.setCountry(req.getCountry());
    merchant.setStateOrProvince(req.getStateOrProvince());
    merchant.setCity(req.getCity());
    merchant.setAddress(req.getAddress());
    merchant.setOpen(false);
    merchant.setAccountId(null);
    merchant.setDiscountSchedule(null);  // or an empty Map
    merchant.setLogoImage("");
    merchant.setStoreImage("");


    // 4) Link Merchant to SignUp
    newSignUp.setMerchant(merchant);


    // 5) Save everything
    signUpService.save(newSignUp); // Cascade = ALL should save Merchant automatically


    // 6) Return the negative Merchant ID
    return ResponseEntity.ok("-" + merchant.getId());
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
                    if (signUp.getMerchant() != null) {
                        // Merchant login
                        return ResponseEntity.ok("" + signUp.getMerchant().getId() * -1);
                    } else if (signUp.getCustomer() != null) {
                        // Customer login
                        return ResponseEntity.ok(signUp.getCustomer().getCustomerId().toString());
                    } else {
                        // No associated customer or merchant
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
                    // Check if it's a merchant account
                    else if (signUp.getMerchant() != null) {
                        Merchant merchant = signUp.getMerchant();
                        merchantService.delete(merchant); // Delete the merchant entity
                        signUpService.delete(signUp); // Delete the sign-up record
                        return ResponseEntity.ok("merchant account deleted");
                    }
                    // If no customer or merchant is associated
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
            helper.setFrom("noreply@barzzy.site"); // SHOULD BE RENAMED TO MEGRIM LATER
            helper.setSubject("Barzzy Verification Code - " + title); // SHOULD BE RENAMED TO MEGRIM LATER


            // Generate hash for the email
            String unsubscribeHash = generateHash(email);
            String unsubscribeUrl = "https://www.barzzy.site/signup/unsubscribe?email=" + email + "&hash=" // SHOULD BE RENAMED TO MEGRIM LATER
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