package edu.help.microservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import edu.help.microservice.dto.ItemCountDTO;
import edu.help.microservice.entity.Order;
import edu.help.microservice.entity.SignUp;
import edu.help.microservice.service.MerchantService;
import edu.help.microservice.service.OrderService;
import edu.help.microservice.service.SignUpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.*;

@RestController
@RequestMapping("/orders")
public class MerchantController {

    private final OrderService orderService;
    private final SignUpService signUpService;
    private final MerchantService merchantService;

    private static final String SECRET_KEY = "YourSecretKey";

    @Autowired
    public MerchantController(OrderService orderService, SignUpService signUpService, MerchantService merchantService) {
        this.orderService = orderService;
        this.signUpService = signUpService;
        this.merchantService = merchantService;
    }



    /**
     * GET /generalData
     * Request Parameters: merchantEmail, merchantPW
     * Response JSON: { "revenue": <double>, "items": <int>, "tips": <double>, "itemsPoints": <int>, "points": <int> }
     */
    @GetMapping("/generalData")
    public ResponseEntity<?> generalData(@RequestParam("merchantEmail") String merchantEmail,
                                         @RequestParam("merchantPW") String merchantPW) {
        try {
            SignUp signUp = signUpService.findByEmail(merchantEmail);
            if (signUp == null || signUp.getMerchant() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("login failed");
            }
            String hashedPassword = hash(merchantPW);
            if (!hashedPassword.equals(signUp.getPasscode())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("login failed");
            }
            int merchantID = signUp.getMerchant().getId();
            List<Order> orders = orderService.getAllOrdersForMerchant(merchantID);
            // Order from most recent to least recent
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

    /**
     * GET /contributionByDateRange
     * Request Parameters: merchantEmail, merchantPW, start, end (timestamps as long)
     * Response JSON: { "orders": [ order1, order2, … orderN] }
     */
    @GetMapping("/contributionByDateRange")
    public ResponseEntity<?> contributionByDateRange(@RequestParam("merchantEmail") String merchantEmail,
                                                     @RequestParam("merchantPW") String merchantPW,
                                                     @RequestParam("start") Long start,
                                                     @RequestParam("end") Long end) {
        try {
            SignUp signUp = signUpService.findByEmail(merchantEmail);
            if (signUp == null || signUp.getMerchant() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("login failed");
            }
            String hashedPassword = hash(merchantPW);
            if (!hashedPassword.equals(signUp.getPasscode())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("login failed");
            }
            int merchantID = signUp.getMerchant().getId();
            Instant startInstant = Instant.ofEpochMilli(start);
            Instant endInstant = Instant.ofEpochMilli(end);

            List<Order> orders = orderService.getOrdersByDateRange(merchantID, startInstant, endInstant);
            // Order from most recent to least recent
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

    /**
     * GET /fiftyOrders
     * Request Parameters: merchantEmail, merchantPW, timestamp (long), index (int)
     * Response JSON: { "orders": [ order1, order2, … orderN] }
     * Returns 50 orders starting from the given timestamp (most recent first).
     */
    @GetMapping("/fiftyOrders")
    public ResponseEntity<?> fiftyOrders(@RequestParam("merchantEmail") String merchantEmail,
                                         @RequestParam("merchantPW") String merchantPW,
                                         @RequestParam("timestamp") Long timestamp,
                                         @RequestParam("index") int index) {
        try {
            SignUp signUp = signUpService.findByEmail(merchantEmail);
            if (signUp == null || signUp.getMerchant() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("login failed");
            }
            String hashedPassword = hash(merchantPW);
            if (!hashedPassword.equals(signUp.getPasscode())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("login failed");
            }
            int merchantID = signUp.getMerchant().getId();
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

    // Helper method for hashing (SHA-256)
    private String hash(String input) throws NoSuchAlgorithmException {
        String text = input + SECRET_KEY;
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(text.getBytes());
        return Base64.getEncoder().encodeToString(hash);
    }



    @GetMapping("/byDay")
    public ResponseEntity<?> byDay(@RequestParam("merchantEmail") String merchantEmail,
                                   @RequestParam("merchantPW") String merchantPW,
                                   @RequestParam("date") String dayStr) { // expects "yyyy-MM-dd"
        try {
            // Authenticate the merchant (same as before)
            SignUp signUp = signUpService.findByEmail(merchantEmail);
            if (signUp == null || signUp.getMerchant() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("login failed");
            }
            String hashedPassword = hash(merchantPW);
            if (!hashedPassword.equals(signUp.getPasscode())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("login failed");
            }
            int merchantID = signUp.getMerchant().getId();

            // Parse the incoming date string assuming format "yyyy-MM-dd"
            // and interpret it as local date in America/New_York (accounting for DST)
            LocalDate localDate = LocalDate.parse(dayStr);
            ZoneId newYorkZone = ZoneId.of("America/New_York");
            ZonedDateTime startOfDayNY = localDate.atStartOfDay(newYorkZone);
            // Convert the ZonedDateTime to a Date (which is in UTC)
            Date day = Date.from(startOfDayNY.toInstant());

            // Get orders for that day (assuming your repository's FUNCTION('DATE', ...) will match)
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


    /**
     * GET /top5Items
     * Request Parameters: merchantEmail, merchantPW
     * Response JSON: { "data": { "itemName1": quantity, "itemName2": quantity, ... } }
     */
    @GetMapping("/top5Items")
    public ResponseEntity<?> top5Items(@RequestParam("merchantEmail") String merchantEmail,
                                        @RequestParam("merchantPW") String merchantPW) {
        try {
            SignUp signUp = signUpService.findByEmail(merchantEmail);
            if (signUp == null || signUp.getMerchant() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("login failed");
            }
            String hashedPassword = hash(merchantPW);
            if (!hashedPassword.equals(signUp.getPasscode())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("login failed");
            }
            int merchantID = signUp.getMerchant().getId();
            Map<String, Integer> top5 = orderService.getTop5Items(merchantID);
            String jsonResponse = "{\"data\":" + new ObjectMapper().writeValueAsString(top5) + "}";
            return ResponseEntity.ok(jsonResponse);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing request");
        }
    }

    @GetMapping("/allItemCounts")
    public ResponseEntity<?> getAllItemCounts(@RequestParam("merchantEmail") String merchantEmail,
                                               @RequestParam("merchantPW") String merchantPW) {
        try {
            SignUp signUp = signUpService.findByEmail(merchantEmail);
            if (signUp == null || signUp.getMerchant() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("login failed");
            }
            String hashedPassword = hash(merchantPW);
            if (!hashedPassword.equals(signUp.getPasscode())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("login failed");
            }

            int merchantId = signUp.getMerchant().getId();
            List<ItemCountDTO> responseList = orderService.getAllItemCountsForMerchant(merchantId);
            return ResponseEntity.ok(Map.of("data", responseList));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing request");
        }
    }


}