package edu.help.microservice.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.Properties;
import java.util.Random;


import edu.help.microservice.util.Cookies;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
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
import edu.help.microservice.entity.Auth;
import edu.help.microservice.service.MerchantService;
import edu.help.microservice.service.CustomerService;
import edu.help.microservice.service.AuthService;
import edu.help.microservice.service.StripeService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.nio.file.Path;
import static edu.help.microservice.util.Cookies.*;
import java.util.UUID;


@RequiredArgsConstructor
@RestController
@RequestMapping("/postgres-test/auth")
public class AuthController {

    /*
    In the context of merchants:
    
    Registration is by WEBSITE ONLY
    Verification is by WEBSITE ONLY
    Login is by WEBSITE(cookies) AND APP(local-storage)
    
    For Customers:
    Registration is by APP ONLY
    Verification is by APP ONLY
    Login is by APP ONLY
    
    
     */

    private final boolean TESTING = false;
    private static final String SECRET_KEY = "YourSecretKey";
    private final AuthService authService;
    private final CustomerService customerService;
    private final MerchantService merchantService;
    private final StripeService stripeService;

    @PostMapping("/register-merchant")
    public ResponseEntity<String> registerMerchant(@RequestParam("email") String email) {

        Auth existingSignUp = authService.findByEmail(email);

        if (existingSignUp != null)
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already exists");

        Auth auth = new Auth();
        auth.setEmail(email);
        auth.setIsMerchant(true);

        String verificationCode = generateVerificationCode();
        try {
            auth.setVerificationCode(hash(verificationCode));
        } catch (NoSuchAlgorithmException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("error generating verification code");
        }

        auth.setExpiryTimestamp(generateExpiryTimestamp());
        authService.save(auth);

        sendVerificationEmail(email, verificationCode, "Registration");
        return ResponseEntity.ok("verification email sent");
    }

    @PostMapping("/verify-merchant")
    public ResponseEntity<String> verifyMerchant(@RequestPart("info") MerchantRegistrationRequest req,
            @RequestPart("logoImage") MultipartFile logoImage,
            @RequestPart(value = "storeImage", required = false) MultipartFile storeImage,
            HttpServletResponse response) {

        String email = req.getEmail();
        Auth auth = authService.findByEmail(email);

        if (auth != null && auth.getMerchant() == null && auth.getIsMerchant()) {
            if (isVerificationCodeExpired(auth.getExpiryTimestamp())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("registration failed: verification code expired");
            }
            try {
                String hashedCode = hash(req.getVerificationCode());
                if (auth.getVerificationCode().equals(hashedCode)) {
                    Merchant merchant = new Merchant();
                    merchant.setName(req.getCompanyName());
                    merchant.setNickname(req.getCompanyNickname());
                    merchant.setCountry(req.getCountry());
                    merchant.setStateOrProvince(req.getStateOrProvince());
                    merchant.setCity(req.getCity());
                    merchant.setAddress(req.getAddress());
                    merchant.setZipCode(req.getZipCode());
                    merchant.setVerified(false);
                    merchant.setBonus(0);

                    String logoImagePath = saveImageFile(logoImage);
                    String storeImagePath = (storeImage != null) ? saveImageFile(storeImage) : logoImagePath;
                    merchant.setLogoImage(logoImagePath);
                    merchant.setStoreImage(storeImagePath);

                    String hashedPassword = hash(req.getPassword());
                    auth.setPasscode(hashedPassword);
                    auth.setVerificationCode(null);
                    auth.setExpiryTimestamp(null);

                    System.out.println("[DEBUG] Creating Stripe account with email: " + email);
        

                    try {
                        String accountId = stripeService.createConnectedAccount(email);

                        System.out.println("[DEBUG] Created Stripe account: " + accountId);
                
                        merchant.setAccountId(accountId);
                    
                        // link back into Auth, set cookie, etc…
                    } catch (StripeException e) {
                        e.printStackTrace();
                        return ResponseEntity
                                .status(HttpStatus.PRECONDITION_FAILED)
                                .body("Stripe error: " + e.getMessage());
                    }
                    
                    merchantService.save(merchant);
                    auth.setMerchant(merchant);
                    authService.save(auth);

                    String id = String.valueOf(merchant.getMerchantId());
                    String expiry = String.valueOf(System.currentTimeMillis() + 3600 * 1000);
                    String payload = id + "." + expiry;
                    String signature = generateSignature(payload);
                    String cookieValueRaw = payload + "." + signature;
                    String cookieValueEncoded = Base64.getEncoder()
                            .encodeToString(cookieValueRaw.getBytes(StandardCharsets.UTF_8));

                    response.addHeader("Set-Cookie", String.format(
                            "auth=%s;  Path=/; HttpOnly; SameSite=Lax;",
                            cookieValueEncoded));

                    return ResponseEntity.ok("-" + merchant.getMerchantId().toString());
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

    @PostMapping("/login-merchant")
    public ResponseEntity<String> loginMerchant(
            @RequestBody LoginRequest loginRequest,
            HttpServletResponse response,
            @CookieValue(value = "auth", required = false) String authCookie) {

        boolean loginSuccessful = false;
        int merchantId = -1;

        // 1. Attempt Cookie Authentication First
        if (authCookie != null) {
            try {
                String decoded = new String(Base64.getDecoder().decode(authCookie), StandardCharsets.UTF_8);
                String[] parts = decoded.split("\\.");

                if (parts.length == 3) {
                    String id = parts[0];
                    String expiry = parts[1];
                    String receivedSignature = parts[2];
                    String signedData = id + "." + expiry;

                    if (System.currentTimeMillis() <= Long.parseLong(expiry)
                            && validateSignature(signedData, receivedSignature)) {
                        merchantId = Integer.parseInt(id);
                        loginSuccessful = true;
                    }
                } else {
                    System.out.println("Invalid cookie parts: " + decoded);
                }
            } catch (Exception e) {
                System.out.println("Invalid cookie format or verification failed.");
                e.printStackTrace();
            }
        }

        // 2. Attempt Email/Password Authentication if Cookie Failed
        if (!loginSuccessful) {
            String email = loginRequest.getEmail();
            String password = loginRequest.getPassword();

            // 2a. If both empty, stay on login page quietly (no error)
            if ((email == null || email.isEmpty()) && (password == null || password.isEmpty())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("EMPTY");
            }

            // 2b. Otherwise, attempt credential verification
            Auth auth = authService.findByEmail(email);
            if (auth != null && auth.getMerchant() != null) {
                try {
                    String hashedPassword = hash(password);
                    if (hashedPassword.equals(auth.getPasscode())) {
                        merchantId = auth.getMerchant().getMerchantId();
                        loginSuccessful = true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // 2c. If credential login failed
            if (!loginSuccessful) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("INVALID_CREDENTIALS");
            }
        }

        // 3. If Authentication Successful → Set a fresh Cookie
        String id = String.valueOf(merchantId);
        String expiry = String.valueOf(System.currentTimeMillis() + 3600 * 1000); // 1 hour later
        String payload = id + "." + expiry;
        System.out.println("Generating signature for cookie for bar id " + merchantId);
        String signature = generateSignature(payload);
        System.out.println("Signature generated for cookie for bar id " + merchantId);

        System.out.println("Signature verified for cookie for bar id " + Cookies.getIdFromCookie(authCookie));

        String cookieValueRaw = payload + "." + signature;
        String cookieValueEncoded = Base64.getEncoder().encodeToString(cookieValueRaw.getBytes(StandardCharsets.UTF_8));

        Cookie cookie = new Cookie("auth", cookieValueEncoded);
        cookie.setMaxAge(3600); // 1 hour
        cookie.setSecure(true); // TODO:Set this to TRUE when in production and FALSE when in testing
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        // Manually override SameSite to Lax for local testing
        response.addHeader("Set-Cookie", String.format(
                "auth=%s; Path=/; Secure; HttpOnly; SameSite=None",
                cookieValueEncoded));

        return ResponseEntity.ok("OK");
    }

    // ENDPOINT: Resend verification code
    @PostMapping("/send-verification")
    public ResponseEntity<String> sendVerification(@RequestBody AcceptTOSRequest request) {
        String email = request.getEmail();
        Auth auth = authService.findByEmail(email);

        if (auth != null) {
            String verificationCode = generateVerificationCode();
            try {
                auth.setVerificationCode(hash(verificationCode));
            } catch (NoSuchAlgorithmException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("error generating verification code");
            }
            auth.setExpiryTimestamp(generateExpiryTimestamp());
            authService.save(auth);
            sendVerificationEmail(email, verificationCode, "Registration");
            return ResponseEntity.ok("verification email sent");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("email not found");
        }
    }

    // ENDPOINT: Verification for Registration
    @PostMapping("/verify-customer")
    public ResponseEntity<String> verifyCustomer(@RequestBody VerificationCustomerRequest verificationRequest) {
        String email = verificationRequest.getEmail();
        String verificationCode = verificationRequest.getVerificationCode();
        String password = verificationRequest.getPassword();
        String firstName = verificationRequest.getFirstName();
        String lastName = verificationRequest.getLastName();
        Auth auth = authService.findByEmail(email);

        if (auth != null && auth.getCustomer() == null) {
            if (isVerificationCodeExpired(auth.getExpiryTimestamp())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("registration failed: verification code expired");
            }
            try {
                String hashedCode = hash(verificationCode);
                if (auth.getVerificationCode().equals(hashedCode)) {
                    // Verification successful
                    Customer customer = new Customer();
                    customer.setFirstName(firstName);
                    customer.setLastName(lastName);

                    // Hash the password and set passcode
                    String hashedPassword = hash(password);
                    auth.setPasscode(hashedPassword);

                    // Clear the verification code and expiry timestamp
                    auth.setVerificationCode(null);
                    auth.setExpiryTimestamp(null);

                    customerService.save(customer); // Save customer

                    auth.setCustomer(customer);
                    authService.save(auth); // Save sign-up details with linked customer

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
    @PostMapping("/register-customer")
    public ResponseEntity<String> registerCustomer(@RequestBody AcceptTOSRequest2 request) {
        String email = request.getEmail();
        Auth existingSignUp = authService.findByEmail(email);

        if (existingSignUp != null)
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already exists");

        // Create new sign-up and customer
        Auth auth = new Auth();
        auth.setEmail(email);
        auth.setIsMerchant(false);

        Customer customer = new Customer();
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());

        // Hash and store the password immediately
        try {
            String hashedPassword = hash(request.getPassword());
            auth.setPasscode(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error hashing password");
        }

        // Create Stripe customer and save the customer ID
        try {
            stripeService.createStripeCustomer(customer, auth);
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
        auth.setCustomer(customer);
        authService.save(auth);

        // Return the customer ID as a string
        return ResponseEntity.ok(String.valueOf(customer.getCustomerId()));
    }

    // ENDPOINT #4: Accept terms of service
    @PostMapping("/accept-tos")
    public ResponseEntity<String> acceptTOS(@RequestBody AcceptTOSRequest request) {
        String email = request.getEmail();
        Auth auth = authService.findByEmail(email);

        if (auth != null && auth.getCustomer() != null) {
            Customer customer = auth.getCustomer();
            customer.setAcceptedTOS(true);
            customerService.save(customer);
            return ResponseEntity.ok("TOS accepted");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("user not found");
        }
    }

    // ENDPOINT #5: Login with email and password
    @PostMapping("/login-customer")
    public ResponseEntity<String> login(@RequestBody LoginRequest loginRequest) {
        String email = loginRequest.getEmail();
        String password = loginRequest.getPassword();
        Auth auth = authService.findByEmail(email);

        if (auth != null) {
            try {
                String hashedPassword = hash(password);
                if (auth.getPasscode() != null && auth.getPasscode().equals(hashedPassword)) {
                    if (auth.getMerchant() != null) {
                        // Merchant login
                        return ResponseEntity.ok("" + auth.getMerchant().getMerchantId() * -1);
                    } else if (auth.getCustomer() != null) {
                        // Customer login
                        return ResponseEntity.ok(auth.getCustomer().getCustomerId().toString());
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
    @PostMapping("/delete-customer")
    public ResponseEntity<String> deleteAccount(@RequestBody LoginRequest loginRequest) {
        String email = loginRequest.getEmail();
        String password = loginRequest.getPassword();
        Auth auth = authService.findByEmail(email);

        if (auth != null) {
            try {
                String hashedPassword = hash(password);

                // Verify the password
                if (auth.getPasscode() != null && auth.getPasscode().equals(hashedPassword)) {
                    // Check if it's a customer account
                    if (auth.getCustomer() != null) {
                        Customer customer = auth.getCustomer();
                        customerService.delete(customer); // Delete the customer entity
                        authService.delete(auth); // Delete the sign-up record
                        return ResponseEntity.ok("customer account deleted");
                    }
                    // Check if it's a merchant account
                    else if (auth.getMerchant() != null) {
                        Merchant merchant = auth.getMerchant();
                        merchantService.delete(merchant); // Delete the merchant entity
                        authService.delete(auth); // Delete the sign-up record
                        return ResponseEntity.ok("merchant account deleted");
                    }
                    // If no customer or merchant is associated
                    else {
                        authService.delete(auth);
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
            String unsubscribeUrl = "https://www.barzzy.site/postgres-production/auth/unsubscribe?email=" + email
                    + "&hash=" // SHOULD BE RENAMED TO MEGRIM LATER
                    + unsubscribeHash;

            // HTML content with clickable link
            String htmlContent =
            "<p>Your verification code is: <strong>" + code + "</strong>"
          + "<span style=\"user-select:none\">.</span></p>"
          + "<p>If you did not wish to receive this email, click here to "
          + "<a href='" + unsubscribeUrl + "'>unsubscribe from all emails</a>.</p>";

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
        Auth auth = authService.findByEmail(email);

        if (auth != null) {
            // Generate a new verification code
            String verificationCode = generateVerificationCode();
            try {
                auth.setVerificationCode(hash(verificationCode));
            } catch (NoSuchAlgorithmException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("error generating verification code");
            }
            auth.setExpiryTimestamp(generateExpiryTimestamp());
            authService.save(auth);
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

        Auth auth = authService.findByEmail(email);

        if (auth != null) {
            if (isVerificationCodeExpired(auth.getExpiryTimestamp())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("verification code expired");
            }
            try {
                String hashedCode = hash(verificationCode);
                if (hashedCode.equals(auth.getVerificationCode())) {
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

        Auth auth = authService.findByEmail(email);

        if (auth != null) {
            if (isVerificationCodeExpired(auth.getExpiryTimestamp())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("verification code expired");
            }
            try {
                String hashedCode = hash(verificationCode);
                if (hashedCode.equals(auth.getVerificationCode())) {
                    // Verification code is correct
                    // Hash the new password and update the passcode
                    String hashedPassword = hash(newPassword);
                    auth.setPasscode(hashedPassword);
                    // Clear the verification code and expiry timestamp
                    auth.setVerificationCode(null);
                    auth.setExpiryTimestamp(null);
                    authService.save(auth);
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

    private String saveImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("Empty file upload!");
        }
        try {
            String uploadsDir = "/uploads/merchants/";
            File dir = new File(uploadsDir);
            if (!dir.exists())
                dir.mkdirs();

            String originalExtension = "";
            String originalName = file.getOriginalFilename();
            if (originalName != null && originalName.contains(".")) {
                originalExtension = originalName.substring(originalName.lastIndexOf("."));
            }

            String fileName = UUID.randomUUID().toString() + originalExtension;
            Path filePath = Paths.get(uploadsDir + fileName);

            Files.copy(file.getInputStream(), filePath);

            return "/uploads/merchants/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save image", e);
        }
    }

}