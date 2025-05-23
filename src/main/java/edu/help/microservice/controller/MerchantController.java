package edu.help.microservice.controller;
import edu.help.microservice.entity.Category;
import edu.help.microservice.repository.CategoryRepository;
import edu.help.microservice.service.*;
import jakarta.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.stripe.StripeClient;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import edu.help.microservice.dto.CreateItemRequestDTO;
import edu.help.microservice.dto.ItemCountDTO;
import edu.help.microservice.dto.UpdateItemRequestDTO;
import edu.help.microservice.entity.Auth;
import edu.help.microservice.entity.Merchant;
import edu.help.microservice.entity.Order;
import edu.help.microservice.repository.AuthRepository;
import edu.help.microservice.repository.MerchantRepository;
import edu.help.microservice.util.Cookies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.*;
import static edu.help.microservice.config.ApiConfig.BASE_PATH;

@RestController
@RequestMapping(BASE_PATH + "/merchant")
public class MerchantController {
    private final OrderService orderService;
    private final MerchantService merchantService;
    private final ItemService itemService;
    private final StripeClient getStripeClient;
    private final MerchantRepository merchantRepository;
    private final AuthRepository authRepository;
    private final CategoryRepository categoryRepository;
    private final S3Service s3Service;

    @Autowired
    public MerchantController(OrderService orderService, AuthService signUpService, MerchantService merchantService,
                              ItemService itemService, StripeClient getStripeClient, MerchantRepository merchantRepository,
                              AuthRepository authRepository, CategoryRepository categoryRepository, S3Service s3Service) {
        this.orderService = orderService;
        this.merchantService = merchantService;
        this.itemService = itemService;
        this.getStripeClient = getStripeClient;
        this.merchantRepository = merchantRepository;
        this.authRepository = authRepository;
        this.s3Service = s3Service;
        this.categoryRepository = categoryRepository;

    }

    @PostMapping("/upload-image-url")
    public ResponseEntity<Map<String, String>> getPresignedImageUploadUrl(
            @CookieValue(value = "auth", required = false) String authCookie,
            @RequestParam String filename
    ) {
        ResponseEntity<Integer> validation = validateAndGetMerchantId(authCookie);
        if (!validation.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(validation.getStatusCode()).build();
        }
        Integer merchantId = validation.getBody();

        String key = merchantId + "/" + filename;

        PresignedPutObjectRequest presigned = s3Service.generatePresignedUrl(key); // no contentType now

        System.out.println("Presigned URL: " + presigned.url());
        System.out.println("Signed headers: " + presigned.signedHeaders()); // should show only {host=[…]}

        return ResponseEntity.ok(Map.of(
                "url", presigned.url().toString(),
                "key", java.net.URLEncoder.encode(key, java.nio.charset.StandardCharsets.UTF_8)
        ));
    }



    @GetMapping("/configurations/categories")
    public ResponseEntity<?> getCategories(@CookieValue(value = "auth", required = false) String authCookie) {
        try {
            ResponseEntity<Integer> validation = validateAndGetMerchantId(authCookie);
            if (!validation.getStatusCode().is2xxSuccessful())
                return validation;

            Integer merchantId = validation.getBody();
            assert merchantId != null;

            List<Category> categories = categoryRepository.findAllByMerchantId(merchantId);
            return ResponseEntity.ok(Map.of("categories", categories));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching categories");
        }
    }


    @PostMapping("/configurations/categories")
    public ResponseEntity<?> addCategories(@CookieValue(value = "auth", required = false) String authCookie,
                                           @RequestBody List<String> categoryNames) {
        try {
            ResponseEntity<Integer> validation = validateAndGetMerchantId(authCookie);
            if (!validation.getStatusCode().is2xxSuccessful())
                return validation;

            Integer merchantId = validation.getBody();
            assert merchantId != null;

            if (categoryNames.size() < 3 || categoryNames.size() > 8) {
                return ResponseEntity.badRequest().body("You must provide between 3 and 8 categories.");
            }

            List<Category> saved = new ArrayList<>();
            for (String name : categoryNames) {
                if (name != null && !name.trim().isEmpty()) {
                    Category c = Category.builder()
                            .merchantId(merchantId)
                            .name(name.trim())
                            .build();
                    saved.add(categoryRepository.save(c));
                }
            }

            return ResponseEntity.ok(Map.of("categories", saved));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error saving categories");
        }
    }

    @PostMapping("/onboarding")
    public ResponseEntity<String> onboarding(@CookieValue(value = "auth", required = false) String authCookie,
            HttpServletRequest request) {
        ResponseEntity<Integer> validation = validateAndGetMerchantId(authCookie);

        if (validation.getStatusCode().equals(HttpStatus.OK))
            return ResponseEntity.ok(null);
        if (!validation.getStatusCode().equals(HttpStatus.FORBIDDEN))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Integer merchantID = validation.getBody();
        if (merchantID == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            return ResponseEntity.status(201).body(createStripeAccountAndGetOnboardingUrl(merchantID));
        } catch (Exception e) {
            e.printStackTrace(); // 👈 ADD THIS LINE
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body(null);
        }
    }

    @GetMapping("/generalData")
    public ResponseEntity<?> generalData(@CookieValue(value = "auth", required = false) String authCookie) {
        try {
            ResponseEntity<Integer> validation = validateAndGetMerchantId(authCookie);
            if (!validation.getStatusCode().is2xxSuccessful())
                return validation;
            Integer merchantID = validation.getBody();
            assert merchantID != null;

            List<Order> orders = orderService.getAllOrdersForMerchant(merchantID);
            orders.sort((o1, o2) -> o2.getTimestamp().compareTo(o1.getTimestamp()));

            double revenue = orders.stream().mapToDouble(Order::getTotalRegularPrice).sum();
            double tips = orders.stream().mapToDouble(Order::getTotalGratuity).sum();
            int items = orders.stream()
                    .flatMap(order -> order.getItems().stream())
                    .mapToInt(itemOrder -> itemOrder.getQuantity())
                    .sum();
            int itemsPoints = orders.stream()
                    .flatMap(order -> order.getItems().stream())
                    .filter(itemOrder -> "points".equalsIgnoreCase(itemOrder.getPaymentType()))
                    .mapToInt(itemOrder -> itemOrder.getQuantity())
                    .sum();
            int pointsSpent = orders.stream().mapToInt(Order::getTotalPointPrice).sum();

            String responseJson = String.format(
                    "{\"revenue\":%.2f, \"items\":%d, \"tips\":%.2f, \"itemsPoints\":%d, \"points\":%d}",
                    revenue, items, tips, itemsPoints, pointsSpent);

            return ResponseEntity.ok(responseJson);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing request");
        }
    }



    @GetMapping("/byDay")
    public ResponseEntity<?> byDay(@CookieValue(value = "auth", required = false) String authCookie,
            @RequestParam("date") String dayStr) { // expects "yyyy-MM-dd"
        try {
            ResponseEntity<Integer> validation = validateAndGetMerchantId(authCookie);
            if (!validation.getStatusCode().is2xxSuccessful())
                return validation;
            Integer merchantID = validation.getBody();
            assert merchantID != null;

            LocalDate localDate = LocalDate.parse(dayStr);
            ZoneId newYorkZone = ZoneId.of("America/New_York");
            ZonedDateTime startOfDayNY = localDate.atStartOfDay(newYorkZone);
            Date day = Date.from(startOfDayNY.toInstant());

            List<Order> orders = orderService.getByDay(merchantID, day);
            orders.sort((o1, o2) -> o2.getTimestamp().compareTo(o1.getTimestamp()));

            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            String ordersJson = mapper.writeValueAsString(orders);
            return ResponseEntity.ok("{\"orders\":" + ordersJson + "}");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing request");
        }
    }


    @GetMapping("/allItemCounts")
    public ResponseEntity<?> getAllItemCounts(@CookieValue(value = "auth", required = false) String authCookie) {
        try {
            ResponseEntity<Integer> validation = validateAndGetMerchantId(authCookie);
            if (!validation.getStatusCode().is2xxSuccessful())
                return validation;
            Integer merchantID = validation.getBody();
            assert merchantID != null;

            List<ItemCountDTO> responseList = orderService.getAllItemCountsForMerchant(merchantID);
            return ResponseEntity.ok(Map.of("data", responseList));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing request");
        }
    }

    /* ---------------------- ROUTES ---------------------- */
    @GetMapping
    public ResponseEntity<?> menu(@CookieValue(value = "auth", required = false) String authCookie) {
        try {
            ResponseEntity<Integer> validation = validateAndGetMerchantId(authCookie);
            if (!validation.getStatusCode().is2xxSuccessful())
                return validation;
            Integer merchantID = validation.getBody();
            assert merchantID != null;

            return ResponseEntity.ok(itemService.getMenu(merchantID));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing request");
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@CookieValue(value = "auth", required = false) String authCookie,
            @RequestBody CreateItemRequestDTO req) {
        try {
            ResponseEntity<Integer> validation = validateAndGetMerchantId(authCookie);
            if (!validation.getStatusCode().is2xxSuccessful())
                return validation;
            Integer merchantID = validation.getBody();
            assert merchantID != null;

            return ResponseEntity.ok(itemService.create(merchantID, req));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating item");
        }
    }

    @PatchMapping("/{itemId}")
    public ResponseEntity<?> update(@CookieValue(value = "auth", required = false) String authCookie,
            @PathVariable Integer itemId,
            @RequestBody UpdateItemRequestDTO req) {
        try {
            ResponseEntity<Integer> validation = validateAndGetMerchantId(authCookie);
            if (!validation.getStatusCode().is2xxSuccessful())
                return validation;
            Integer merchantID = validation.getBody();
            assert merchantID != null;

            return ResponseEntity.ok(itemService.update(merchantID, itemId, req));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating item");
        }
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<?> delete(@CookieValue(value = "auth", required = false) String authCookie,
            @PathVariable Integer itemId) {
        try {
            ResponseEntity<Integer> validation = validateAndGetMerchantId(authCookie);
            if (!validation.getStatusCode().is2xxSuccessful())
                return validation;
            Integer merchantID = validation.getBody();
            assert merchantID != null;

            itemService.delete(merchantID, itemId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Extracts and validates the merchant ID from the cookie.
     * Returns ResponseEntity with proper error if any validation fails:
     * - 401 Unauthorized: if cookie is missing, expired, invalid, or signature check fails.
     * - 403 Forbidden: if merchant ID exists but no associated merchant (not onboarded).
     */
    private ResponseEntity<Integer> validateAndGetMerchantId(String authCookie) {
        if (authCookie == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        try {
            String decoded = new String(Base64.getDecoder().decode(authCookie), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\.");
            if (parts.length != 3)
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);

            String id = parts[0];
            String expiry = parts[1];
            String signature = parts[2];

            if (signature == null || signature.isEmpty()) { // TODO: Unauthorized = login failure redirect to /login
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }

            if (System.currentTimeMillis() > Long.parseLong(expiry)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }

            String payload = id + "." + expiry;
            if (!Cookies.validateSignature(payload, signature)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }

            int merchantId = Integer.parseInt(id);
            Merchant merchant = merchantService.findMerchantById(merchantId);
            if (merchant == null || merchant.getAccountId() == null) {
                System.out.println("[DEBUG] Merchant not onboarded for merchantId: " + merchantId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(merchantId);
            }

            // Merchant exists and has an accountId → consider them onboarded
            return ResponseEntity.ok(merchantId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }

    public String createStripeAccountAndGetOnboardingUrl(Integer merchantId)
            throws Exception {
        System.out.println("[DEBUG] Starting createStripeAccountAndGetOnboardingUrl for merchantId: " + merchantId);

        // Step 1: Retrieve merchant and auth
        Merchant m = merchantRepository.getMerchantsByMerchantId(merchantId);
        System.out.println("[DEBUG] Retrieved merchant: " + m);

        Optional<Auth> a2 = authRepository.findByMerchant_MerchantId(merchantId);
        if (a2.isEmpty()) {
            System.err.println("[ERROR] Auth record not found for merchantId: " + merchantId);
            throw new IllegalStateException("Auth record missing");
        }

        Auth a = a2.get();
        System.out.println("[DEBUG] Retrieved auth: " + a);

        // Step 2: Create the Stripe account
        System.out.println("[DEBUG] Creating Stripe account with email: " + a.getEmail());
        AccountCreateParams accountParams = AccountCreateParams.builder()
                .setType(AccountCreateParams.Type.EXPRESS)
                .setCountry("US")
                .setEmail(a.getEmail())
                .setCapabilities(
                        AccountCreateParams.Capabilities.builder()
                                .setCardPayments(AccountCreateParams.Capabilities.CardPayments.builder()
                                        .setRequested(true)
                                        .build())
                                .setTransfers(AccountCreateParams.Capabilities.Transfers.builder()
                                        .setRequested(true)
                                        .build())
                                .build())
                .build();

        Account account = getStripeClient.accounts().create(accountParams);
        System.out.println("[DEBUG] Created Stripe account: " + account.getId());

        // Step 3: Save account ID to merchant
        m.setAccountId(account.getId());
        merchantService.save(m);
        System.out.println("[DEBUG] Saved account ID to merchant: " + account.getId());

        // Step 4: Generate onboarding link
        System.out.println("[DEBUG] Creating onboarding link");
        AccountLinkCreateParams linkParams = AccountLinkCreateParams.builder()
                .setAccount(account.getId())
                .setRefreshUrl("https://barzzy.site/website/onboarding")
                .setReturnUrl("https://barzzy.site/website/analytics")
                .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                .setCollect(AccountLinkCreateParams.Collect.EVENTUALLY_DUE)
                .build();

        AccountLink accountLink = getStripeClient.accountLinks().create(linkParams);
        System.out.println("[DEBUG] Generated onboarding link: " + accountLink.getUrl());

        return accountLink.getUrl();
    }

}
