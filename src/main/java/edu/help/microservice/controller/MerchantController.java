package edu.help.microservice.controller;

import edu.help.microservice.entity.Category;
import edu.help.microservice.repository.CategoryRepository;
import edu.help.microservice.repository.EmployeeRepository;
import edu.help.microservice.service.*;
import jakarta.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.stripe.StripeClient;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.param.AccountLinkCreateParams;
import edu.help.microservice.dto.CreateItemRequestDTO;
import edu.help.microservice.dto.ItemCountDTO;
import edu.help.microservice.dto.UpdateItemRequestDTO;
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
import com.stripe.param.AccountRetrieveParams;
import com.stripe.model.Account.Requirements;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.*;


@RestController
@RequestMapping("/merchant")
public class MerchantController {
    private final OrderService orderService;
    private final MerchantService merchantService;
    private final ItemService itemService;
    private final StripeClient getStripeClient;
    private final MerchantRepository merchantRepository;
    private final CategoryRepository categoryRepository;
    private final S3Service s3Service;
    

    @Autowired
    public MerchantController(OrderService orderService, AuthService signUpService, MerchantService merchantService,
            ItemService itemService, StripeClient getStripeClient, MerchantRepository merchantRepository,
            AuthRepository authRepository, CategoryRepository categoryRepository, S3Service s3Service, EmployeeRepository employeeRepository) {
        this.orderService = orderService;
        this.merchantService = merchantService;
        this.itemService = itemService;
        this.getStripeClient = getStripeClient;
        this.merchantRepository = merchantRepository;
        this.s3Service = s3Service;
        this.categoryRepository = categoryRepository;
    }


    @PatchMapping("/configurations/store-image")
    public ResponseEntity<?> updateStoreImage(
            @CookieValue(value = "auth", required = false) String authCookie,
            @RequestBody Map<String, String> body) {

        ResponseEntity<Integer> validation = validateAndGetMerchantId(authCookie);
        if (!validation.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(validation.getStatusCode()).build();
        }

        Integer merchantId = validation.getBody();
        String storeImageUrl = body.get("storeImage");
        if (storeImageUrl == null || storeImageUrl.isBlank()) {
            return ResponseEntity.badRequest().body("Missing image URL");
        }

        merchantRepository.updateStoreImageById(merchantId, storeImageUrl);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/configurations/discount")
    public ResponseEntity<?> getDiscountSchedule(
            @CookieValue(value = "auth", required = false) String authCookie) {
        ResponseEntity<Integer> check = validateAndGetMerchantId(authCookie);
        if (!check.getStatusCode().is2xxSuccessful()) return check;

        Merchant m = merchantService.findMerchantById(check.getBody());
        String json = m.getDiscountSchedule();            // may be null
        return ResponseEntity.ok(Map.of("discountSchedule",
                json == null || json.isBlank() ? "{}" : json));
    }

    @PostMapping("/configurations/discount")
    public ResponseEntity<?> saveDiscountSchedule(
            @CookieValue(value = "auth", required = false) String authCookie,
            @RequestBody Map<String, String> req) {        // keys = day names, value = null or "hh:mm - hh:mm | …"
        ResponseEntity<Integer> check = validateAndGetMerchantId(authCookie);
        if (!check.getStatusCode().is2xxSuccessful()) return check;

        try {
            Merchant m = merchantService.findMerchantById(check.getBody());

            // very light sanity: all 7 day keys allowed, value may be null or ≤ 120 chars
            for (String k : req.keySet()) {
                if (!List.of("Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday").contains(k))
                    return ResponseEntity.badRequest().body("Invalid day key: " + k);
                String v = req.get(k);
                if (v != null && v.length() > 120)
                    return ResponseEntity.badRequest().body("Value too long for " + k);
            }

            m.setDiscountSchedule(new ObjectMapper().writeValueAsString(req));
            merchantService.save(m);
            return ResponseEntity.ok(Map.of("discountSchedule", m.getDiscountSchedule()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to save schedule");
        }
    }

    /* ───────── CATEGORY DELETE ───────── */

    @DeleteMapping("/configurations/categories/{id}")
    public ResponseEntity<?> deleteCategory(
            @CookieValue(value = "auth", required = false) String authCookie,
            @PathVariable Integer id) {
        ResponseEntity<Integer> check = validateAndGetMerchantId(authCookie);
        if (!check.getStatusCode().is2xxSuccessful()) return check;

        categoryRepository.deleteByIdAndMerchantId(id, check.getBody());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/upload-image-url")
    public ResponseEntity<Map<String, String>> getPresignedImageUploadUrl(
            @CookieValue(value = "auth", required = false) String authCookie,
            @RequestParam String filename) {
        ResponseEntity<Integer> validation = validateAndGetMerchantId(authCookie);
        if (!validation.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(validation.getStatusCode()).build();
        }
        Integer merchantId = validation.getBody();

        // build the S3 key under this merchant’s folder
        String key = merchantId + "/" + filename;

        PresignedPutObjectRequest presigned = s3Service.generatePresignedUrl(key); // no contentType now

        System.out.println("Presigned URL: " + presigned.url());
        System.out.println("Signed headers: " + presigned.signedHeaders()); // should show only {host=[…]}

        return ResponseEntity.ok(Map.of(
                "url", presigned.url().toString(),
                "key", java.net.URLEncoder.encode(key, java.nio.charset.StandardCharsets.UTF_8)));
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

            if (categoryNames.size() < 3 || categoryNames.size() > 10) {
                return ResponseEntity.badRequest().body("You must provide between 3 and 10 categories.");
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
    public ResponseEntity<String> onboarding(
            @CookieValue(value = "auth", required = false) String authCookie,
            HttpServletRequest request) {

        ResponseEntity<Integer> validation = validateAndGetMerchantId(authCookie);

        if (!validation.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(validation.getStatusCode()).build();
        }

        Integer merchantID = validation.getBody();
        if (merchantID == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // Step 1: Retrieve merchant and auth
            Merchant m = merchantRepository.getMerchantsByMerchantId(merchantID);
            System.out.println("[DEBUG] Retrieved merchant: " + m);

            if (m.getAccountId() == null || m.getAccountId().isEmpty()) {
                System.err.println("[ERROR] No Stripe accountId found for merchantId: " + merchantID);
                return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body("No Stripe accountId on record");
            }

            // Step 2: Generate onboarding link
            System.out.println("[DEBUG] Creating onboarding link for existing account: " + m.getAccountId());
            AccountLinkCreateParams linkParams = AccountLinkCreateParams.builder()
                    .setAccount(m.getAccountId())
                    .setRefreshUrl("https://megrim.com/onboarding")
                    .setReturnUrl("https://megrim.com/inventory")
                    .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                    .setCollect(AccountLinkCreateParams.Collect.EVENTUALLY_DUE)
                    .build();

            AccountLink accountLink = getStripeClient.accountLinks().create(linkParams);
            System.out.println("[DEBUG] Generated onboarding link: " + accountLink.getUrl());

            return ResponseEntity.ok(accountLink.getUrl());
        } catch (Exception e) {
            e.printStackTrace();
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
     * - 401 Unauthorized: if cookie is missing, expired, invalid, or signature
     * check fails.
     * - 403 Forbidden: if merchant ID exists but no associated merchant (not
     * onboarded).
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

            if (signature == null || signature.isEmpty()) {
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
            if (merchant == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }

            // Merchant exists and has an accountId → consider them onboarded
            return ResponseEntity.ok(merchantId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }

    @GetMapping("/stripeStatus")
    public ResponseEntity<Map<String, String>> getStripeVerificationStatus(
            @CookieValue(value = "auth", required = false) String authCookie) {
        try {
            ResponseEntity<Integer> validation = validateAndGetMerchantId(authCookie);
            if (!validation.getStatusCode().is2xxSuccessful())
                return ResponseEntity.status(validation.getStatusCode()).build();

            Integer merchantId = validation.getBody();
            Merchant merchant = merchantService.findMerchantById(merchantId);

            if (merchant.getAccountId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "No Stripe account ID found"));
            }

            // Use AccountRetrieveParams to expand "requirements"
            Account account = getStripeClient.accounts().retrieve(
                    merchant.getAccountId(),
                    AccountRetrieveParams.builder()
                            .addExpand("requirements")
                            .build());

            Requirements requirements = account.getRequirements();
            String status = "verified";

            if (requirements != null && requirements.getDisabledReason() != null) {
                status = requirements.getDisabledReason();
            }

            merchant.setStripeVerificationStatus(status);
            merchantService.save(merchant);

            return ResponseEntity.ok(Map.of("stripe_verification_status", status));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Stripe verification status check failed"));
        }
    }
}
