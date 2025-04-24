package edu.help.microservice.controller;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
import edu.help.microservice.dto.MerchantRegistrationRequest;
import edu.help.microservice.dto.LoginRequest;
import edu.help.microservice.dto.ResetPasswordConfirmRequest;
import edu.help.microservice.dto.VerificationMerchantRequest;       // For "this hour"
import edu.help.microservice.dto.VerificationRequest; // If needed for logging
import edu.help.microservice.dto.VerifyResetCodeRequest;
import edu.help.microservice.entity.Activity;
import edu.help.microservice.entity.Merchant;
import edu.help.microservice.entity.Customer;
import edu.help.microservice.entity.SignUp;
import edu.help.microservice.entity.SubscriptionInfo;
import edu.help.microservice.service.ActivityService;
import edu.help.microservice.service.MerchantService;
import edu.help.microservice.service.CustomerService;
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
    private final MerchantService merchantService;
    private final StripeService stripeService;
    private final ActivityService activityService;
//BAR REGISTRATION/LOGIN STUFF HERE
///________________________________________________________________
@PostMapping("/registermerchant")
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
    merchant.setMerchantEmail(req.getEmail());
    merchant.setMerchantName(req.getCompanyName());
    merchant.setMerchantTag(req.getCompanyNickname());
    merchant.setMerchantCountry(req.getCountry());
    merchant.setMerchantState(req.getRegion());
    merchant.setMerchantCity(req.getCity());
    merchant.setMerchantAddress(req.getAddress());
   
    // Combine open & close time into openHours, or adapt as needed
    merchant.setOpenHours(req.getOpenTime() + " - " + req.getCloseTime());
   
    // Set optional fields to null or defaults
    merchant.setAccountId(null);
    merchant.setSubId(null);
    merchant.setRewardsSubId(null);
    merchant.setHappyHourTimes(null);  // or an empty Map
    merchant.setStartDate(null);
    merchant.setTagImage("");
    merchant.setMerchantImage("");


    // 4) Link Merchant to SignUp
    newSignUp.setMerchant(merchant);


    // 5) Save everything
    signUpService.save(newSignUp); // Cascade = ALL should save Merchant automatically


    // 6) Return the negative Merchant ID
    return ResponseEntity.ok("-" + merchant.getMerchantId());
}




    @PostMapping("/subscriptionChange")
    public ResponseEntity<String> subscriptionChange(
            @RequestParam("userId") Integer userId,
            @RequestParam("merchantId") Integer merchantId,
            @RequestParam("subscribe") boolean subscribe) {


        // Retrieve the customer using your customerService (assumes you have findById method)
        Optional<Customer> customer2 = customerService.findById(userId);
        if (customer2.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Customer not found");
        }
        Customer customer = customer2.get();


        // Retrieve the subscription map from the customer entity.
        // This map uses merchant IDs as keys and SubscriptionInfo objects as values.
        Map<Integer, SubscriptionInfo> subscriptions = customer.getSubscription();
        if (subscriptions == null) {
            subscriptions = new HashMap<>();


            customer.setSubscription(subscriptions);
        }


        // Retrieve the SubscriptionInfo for the given merchantId; create a default if not found.
        SubscriptionInfo subscriptionInfo = subscriptions.get(merchantId);
        if (subscriptionInfo == null) {
            subscriptionInfo = new SubscriptionInfo();


            // TODO: Update values, update whatever updates users points to use SUB column instead of points column
            subscriptionInfo.setIsSubscribed(false);
            subscriptionInfo.setPoints(0);


            subscriptions.put(merchantId, subscriptionInfo);
        }


        // Update subscription info based on the subscribe flag.
        if (subscribe) {
            subscriptionInfo.setIsSubscribed(true);


            // TODO: STRIPE LOGIC
        } else {
            subscriptionInfo.setIsSubscribed(false);


        }


        // Save the updated customer record.
        customerService.save(customer);


        return ResponseEntity.ok("Subscription updated successfully");
    }


    /**
     * The pay-to-use "heartbeat" call from the frontend.
     *  1) if startDate == null, set it to now -> stop
     *  2) if startDate < 30 days old, do nothing -> stop
     *  3) otherwise, record usage for this hour (placeholder).
     */
    @PostMapping("/heartbeat")
    public ResponseEntity<String> heartbeat(@RequestParam("merchantId") String merchantID,
            @RequestParam("stationId") String stationID) {
        System.out.println("heartbeat initiated for merchant " + merchantID);
        try {
            int merchantIdInt = Integer.parseInt(merchantID);
            Merchant merchant = merchantService.findMerchantById(merchantIdInt);
            if (merchant == null) {
                System.out.println("heartbeat: no merchant found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No merchant found with ID " + merchantID);
            }


            System.out.println("heartbeat: Merchant Found");


            // 1) If merchant.startDate is null, set it and stop
            if (merchant.getStartDate() == null) {


                System.out.println("heartbeat: attempting startdate setting.");
                merchantService.setStartDate(merchantIdInt, LocalDate.now());
                System.out.println("heartbeat: Started free trial");
                return ResponseEntity.ok("startDate was null, now set to today. Done.");
            }


            // 2) Check how long ago startDate was
            LocalDate startDate = merchant.getStartDate();
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


            // Check if we have an entry for (merchantIdInt, stationID, currentHour)
            if (!activityService.alreadyRecordedThisHour(merchantIdInt, stationID, currentHour)) {
                Activity a1 = activityService.recordActivity(merchantIdInt, stationID, currentHour);
                String debugMessage = String.format("Recorded usage for merchant %d, station %s at hour %s",
                        merchantIdInt, stationID, currentHour.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                if (a1.getActivityId() != null) {
                    System.out.println("heartbeat: Recorded: " + debugMessage);


                    System.out.println("Attempting stripe charge");
                    stripeService.sendMeterEvent(merchant);
                    System.out.println("Stripe charge probably successful");
                }
                return ResponseEntity.ok(debugMessage);
            } else {
                System.out.println("heartbeat: Within Hour");
                return ResponseEntity.ok("Usage for this hour was already recorded. Nothing to do.");
            }
        } catch (Exception e) {
            System.out.println("heartbeat: failed with stacktrace: " + e.getStackTrace() + " and also this: "
                    + e.getCause() + " anddddd this... : " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("heartbeat: failed with stacktrace: " + e.getStackTrace() + " and also this: " + e.getCause()
                            + " anddddd this... : " + e.getMessage());
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


        //TODO: REMOVE THIS WHENEVER WERE NOT DOING PROMOTION
        Map<Integer, Map<Integer, Integer>> pointsMap = customer.getPoints();
        if (pointsMap == null) {
            pointsMap = new HashMap<>();
            customer.setPoints(pointsMap);
        }
        Map<Integer, Integer> userPoints = pointsMap.getOrDefault(customer.getCustomerID(), new HashMap<>());
        userPoints.put(95, 150); // Assign 150 points for merchant id 95
        pointsMap.put(customer.getCustomerID(), userPoints);
        customerService.save(customer);


        // Return the customer ID as a string
        return ResponseEntity.ok(String.valueOf(customer.getCustomerID()));
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
                    merchant.setMerchantEmail(email);
                    merchant.setMerchantName(verificationRequest.getCompanyName());
                    merchant.setMerchantTag(verificationRequest.getCompanyNickname());
                    merchant.setMerchantCountry(verificationRequest.getCountry());
                    merchant.setMerchantState(verificationRequest.getRegion());
                    merchant.setMerchantCity(verificationRequest.getCity());
                    merchant.setMerchantAddress(verificationRequest.getAddress());
                    merchant.setTagImage(""); // Set default or handle accordingly
                    merchant.setMerchantImage(""); // Set default or handle accordingly
                    merchant.setOpenHours(verificationRequest.getOpenTime() + " - " + verificationRequest.getCloseTime());


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
                        return ResponseEntity.ok("" + signUp.getMerchant().getMerchantId() * -1);
                    } else if (signUp.getCustomer() != null) {
                        // Customer login
                        return ResponseEntity.ok(signUp.getCustomer().getCustomerID().toString());
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
            helper.setFrom("noreply@merchantzzy.site");
            helper.setSubject("Merchantzzy Verification Code - " + title);


            // Generate hash for the email
            String unsubscribeHash = generateHash(email);
            String unsubscribeUrl = "https://www.merchantzzy.site/signup/unsubscribe?email=" + email + "&hash="
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

