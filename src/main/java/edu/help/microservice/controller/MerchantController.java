package edu.help.microservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import edu.help.microservice.dto.CreateItemRequestDTO;
import edu.help.microservice.dto.ItemCountDTO;
import edu.help.microservice.dto.ItemDTO;
import edu.help.microservice.dto.UpdateItemRequestDTO;
import edu.help.microservice.entity.Auth;
import edu.help.microservice.entity.Order;
import edu.help.microservice.service.ItemService;
import edu.help.microservice.service.MerchantService;
import edu.help.microservice.service.OrderService;
import edu.help.microservice.service.AuthService;
import edu.help.microservice.util.Cookies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.*;

@RestController
@RequestMapping("/postgres-test/merchant")
public class MerchantController {
    private final OrderService orderService;
    private final AuthService authService;
    private final MerchantService merchantService;
    private final ItemService itemService;
   
    

    @Autowired
    public MerchantController(OrderService orderService, AuthService signUpService, MerchantService merchantService, ItemService itemService) {
        this.orderService = orderService;
        this.authService = signUpService;
        this.merchantService = merchantService;
        this.itemService = itemService;
    }

    @GetMapping("/generalData")
    public ResponseEntity<?> generalData(@CookieValue(value = "auth", required = false) String authCookie) {
        try {
            ResponseEntity<Integer> validation = validateAndGetMerchantId(authCookie);
            if (!validation.getStatusCode().is2xxSuccessful()) return validation;
            Integer merchantID = validation.getBody();
            assert merchantID != null;

            List<Order> orders = orderService.getAllOrdersForMerchant(merchantID);
            orders.sort((o1, o2) -> o2.getTimestamp().compareTo(o1.getTimestamp()));

            double revenue = orders.stream().mapToDouble(Order::getTotalRegularPrice).sum();
            double tips = orders.stream().mapToDouble(Order::getTip).sum();
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

    @GetMapping("/contributionByDateRange")
    public ResponseEntity<?> contributionByDateRange(@CookieValue(value = "auth", required = false) String authCookie,
                                                     @RequestParam("start") Long start,
                                                     @RequestParam("end") Long end) {
        try {
            ResponseEntity<Integer> validation = validateAndGetMerchantId(authCookie);
            if (!validation.getStatusCode().is2xxSuccessful()) return validation;
            Integer merchantID = validation.getBody();
            assert merchantID != null;

            Instant startInstant = Instant.ofEpochMilli(start);
            Instant endInstant = Instant.ofEpochMilli(end);

            List<Order> orders = orderService.getOrdersByDateRange(merchantID, startInstant, endInstant);
            orders.sort((o1, o2) -> o2.getTimestamp().compareTo(o1.getTimestamp()));

            ObjectMapper mapper = new ObjectMapper();
            String ordersJson = mapper.writeValueAsString(orders);
            return ResponseEntity.ok("{\"orders\":" + ordersJson + "}");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing request");
        }
    }

    @GetMapping("/fiftyOrders")
    public ResponseEntity<?> fiftyOrders(@CookieValue(value = "auth", required = false) String authCookie,
                                         @RequestParam("timestamp") Long timestamp,
                                         @RequestParam("index") int index) {
        try {
            ResponseEntity<Integer> validation = validateAndGetMerchantId(authCookie);
            if (!validation.getStatusCode().is2xxSuccessful()) return validation;
            Integer merchantID = validation.getBody();
            assert merchantID != null;

            Instant startingInstant = Instant.ofEpochMilli(timestamp);

            List<Order> orders = orderService.getFiftyOrders(merchantID, startingInstant, index);
            orders.sort((o1, o2) -> o2.getTimestamp().compareTo(o1.getTimestamp()));

            ObjectMapper mapper = new ObjectMapper();
            String ordersJson = mapper.writeValueAsString(orders);
            return ResponseEntity.ok("{\"orders\":" + ordersJson + "}");
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
            if (!validation.getStatusCode().is2xxSuccessful()) return validation;
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

    @GetMapping("/top5Items")
    public ResponseEntity<?> top5Items(@CookieValue(value = "auth", required = false) String authCookie) {
        try {
            ResponseEntity<Integer> validation = validateAndGetMerchantId(authCookie);
            if (!validation.getStatusCode().is2xxSuccessful()) return validation;
            Integer merchantID = validation.getBody();
            assert merchantID != null;
            
            

            Map<String, Integer> top5 = orderService.getTop5Items(merchantID);
            String jsonResponse = "{\"data\":" + new ObjectMapper().writeValueAsString(top5) + "}";
            return ResponseEntity.ok(jsonResponse);
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
            if (!validation.getStatusCode().is2xxSuccessful()) return validation;
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
            if (!validation.getStatusCode().is2xxSuccessful()) return validation;
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
            if (!validation.getStatusCode().is2xxSuccessful()) return validation;
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
            if (!validation.getStatusCode().is2xxSuccessful()) return validation;
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
            if (!validation.getStatusCode().is2xxSuccessful()) return validation;
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
            if (parts.length != 3) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);

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
            Optional<Auth> auth = authService.findById(merchantId);
            if (auth.isEmpty() || auth.get().getMerchant() == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            }

            return ResponseEntity.ok(merchantId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }




}
