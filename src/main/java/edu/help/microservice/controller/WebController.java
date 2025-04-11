package edu.help.microservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.help.microservice.dto.GetTipsResponse;
import edu.help.microservice.dto.OrderDTO;
import edu.help.microservice.dto.TipClaimRequest;
import edu.help.microservice.entity.Order;
import edu.help.microservice.entity.SignUp;
import edu.help.microservice.service.BarService;
import edu.help.microservice.service.OrderService;
import edu.help.microservice.service.SignUpService;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/orders")
public class WebController {

    private final OrderService orderService;
    private final SignUpService signUpService;
    private final BarService barService;
    private static final String SECRET_KEY = "YourSecretKey";

    @Autowired
    public WebController(OrderService orderService, SignUpService signUpService, BarService barService) {
        this.orderService = orderService;
        this.signUpService = signUpService;
        this.barService = barService;
    }

    /**
     * GET /generalData
     * Request Parameters: barEmail, barPW
     * Response JSON: { "revenue": <double>, "drinks": <int>, "tips": <double>, "drinksPoints": <int>, "points": <int> }
     */
    @GetMapping("/generalData")
    public ResponseEntity<?> generalData(@RequestParam("barEmail") String barEmail,
                                         @RequestParam("barPW") String barPW) {
        try {
            SignUp signUp = signUpService.findByEmail(barEmail);
            if (signUp == null || signUp.getBar() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("login failed");
            }
            String hashedPassword = hash(barPW);
            if (!hashedPassword.equals(signUp.getPasscode())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("login failed");
            }
            int barID = signUp.getBar().getBarId();
            List<Order> orders = orderService.getAllOrdersForBar(barID);
            // Order from most recent to least recent
            orders.sort((o1, o2) -> o2.getTimestamp().compareTo(o1.getTimestamp()));

            double revenue = orders.stream().mapToDouble(Order::getTotalRegularPrice).sum();
            double tips = orders.stream().mapToDouble(Order::getTip).sum();
            int drinks = orders.stream()
                    .flatMap(order -> order.getDrinks().stream())
                    .mapToInt(drinkOrder -> drinkOrder.getQuantity())
                    .sum();
            int drinksPoints = orders.stream()
                    .flatMap(order -> order.getDrinks().stream())
                    .filter(drinkOrder -> "points".equalsIgnoreCase(drinkOrder.getPaymentType()))
                    .mapToInt(drinkOrder -> drinkOrder.getQuantity())
                    .sum();
            int pointsSpent = orders.stream().mapToInt(Order::getTotalPointPrice).sum();

            String responseJson = String.format(
                    "{\"revenue\":%.2f, \"drinks\":%d, \"tips\":%.2f, \"drinksPoints\":%d, \"points\":%d}",
                    revenue, drinks, tips, drinksPoints, pointsSpent);

            return ResponseEntity.ok(responseJson);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing request");
        }
    }

    /**
     * GET /contributionByDateRange
     * Request Parameters: barEmail, barPW, start, end (timestamps as long)
     * Response JSON: { "orders": [ order1, order2, … orderN] }
     */
    @GetMapping("/contributionByDateRange")
    public ResponseEntity<?> contributionByDateRange(@RequestParam("barEmail") String barEmail,
                                                     @RequestParam("barPW") String barPW,
                                                     @RequestParam("start") Long start,
                                                     @RequestParam("end") Long end) {
        try {
            SignUp signUp = signUpService.findByEmail(barEmail);
            if (signUp == null || signUp.getBar() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("login failed");
            }
            String hashedPassword = hash(barPW);
            if (!hashedPassword.equals(signUp.getPasscode())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("login failed");
            }
            int barID = signUp.getBar().getBarId();
            Instant startInstant = Instant.ofEpochMilli(start);
            Instant endInstant = Instant.ofEpochMilli(end);

            List<Order> orders = orderService.getOrdersByDateRange(barID, startInstant, endInstant);
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
     * Request Parameters: barEmail, barPW, timestamp (long), index (int)
     * Response JSON: { "orders": [ order1, order2, … orderN] }
     * Returns 50 orders starting from the given timestamp (most recent first).
     */
    @GetMapping("/fiftyOrders")
    public ResponseEntity<?> fiftyOrders(@RequestParam("barEmail") String barEmail,
                                         @RequestParam("barPW") String barPW,
                                         @RequestParam("timestamp") Long timestamp,
                                         @RequestParam("index") int index) {
        try {
            SignUp signUp = signUpService.findByEmail(barEmail);
            if (signUp == null || signUp.getBar() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("login failed");
            }
            String hashedPassword = hash(barPW);
            if (!hashedPassword.equals(signUp.getPasscode())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("login failed");
            }
            int barID = signUp.getBar().getBarId();
            Instant startingInstant = Instant.ofEpochMilli(timestamp);

            List<Order> orders = orderService.getFiftyOrders(barID, startingInstant, index);
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






    /**
     * 
     */
    @GetMapping("/byDay")
    public ResponseEntity<?> byDay(@RequestParam("barEmail") String barEmail,
                                         @RequestParam("barPW") String barPW,
                                         @RequestParam("date") Date day) {
        try {
            SignUp signUp = signUpService.findByEmail(barEmail);
            if (signUp == null || signUp.getBar() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("login failed");
            }
            String hashedPassword = hash(barPW);
            if (!hashedPassword.equals(signUp.getPasscode())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("login failed");
            }
            int barID = signUp.getBar().getBarId();
            
            
            

            List<Order> orders = orderService.getByDay(barID, day);
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
     * GET /top5Drinks
     * Request Parameters: barEmail, barPW
     * Response JSON: { "data": { "drinkName1": quantity, "drinkName2": quantity, ... } }
     */
    @GetMapping("/top5Drinks")
    public ResponseEntity<?> top5Drinks(@RequestParam("barEmail") String barEmail,
                                        @RequestParam("barPW") String barPW) {
        try {
            SignUp signUp = signUpService.findByEmail(barEmail);
            if (signUp == null || signUp.getBar() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("login failed");
            }
            String hashedPassword = hash(barPW);
            if (!hashedPassword.equals(signUp.getPasscode())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("login failed");
            }
            int barID = signUp.getBar().getBarId();
            Map<String, Integer> top5 = orderService.getTop5Drinks(barID);
            String jsonResponse = "{\"data\":" + new ObjectMapper().writeValueAsString(top5) + "}";
            return ResponseEntity.ok(jsonResponse);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing request");
        }
    }

    
}
