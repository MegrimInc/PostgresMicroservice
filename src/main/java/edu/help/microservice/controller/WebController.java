package edu.help.microservice.controller;

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
import java.util.Base64;
import java.util.List;
import java.util.Properties;

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
            // Retrieve the barId from the bar object
            int barID = signUp.getBar().getBarId();

            // Get all orders for this bar
            List<Order> orders = orderService.getAllOrdersForBar(barID);
            // Order the orders from most recent to least recent
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

            // Build response JSON (profit = revenue - tips if needed)
            String responseJson = String.format("{\"revenue\":%.2f, \"drinks\":%d, \"tips\":%.2f, \"drinksPoints\":%d, \"points\":%d}",
                    revenue, drinks, tips, drinksPoints, pointsSpent);
            return ResponseEntity.ok(responseJson);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing request");
        }
    }

    /**
     * GET /contributionByDateRange
     * Request Parameters: barEmail, barPW, start, end (timestamps as long)
     * Response JSON: { "orders": [ order1, order2, … ] }
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
            // Convert the long timestamps to Instant
            Instant startInstant = Instant.ofEpochMilli(start);
            Instant endInstant = Instant.ofEpochMilli(end);

            List<Order> orders = orderService.getOrdersByDateRange(barID, startInstant, endInstant);
            // Order from most recent to least recent
            orders.sort((o1, o2) -> o2.getTimestamp().compareTo(o1.getTimestamp()));

            // Build JSON response
            // (Assuming Order has proper getters and your JSON library serializes it correctly)
            return ResponseEntity.ok("{\"orders\":" + new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(orders) + "}");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing request");
        }
    }

    /**
     * GET /fiftyOrders
     * Request Parameters: barEmail, barPW, timestamp (long), index (int)
     * Response JSON: { "orders": [ order1, order2, … ] }
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
            // Convert the given timestamp to an Instant
            Instant startingInstant = Instant.ofEpochMilli(timestamp);
            // Retrieve 50 orders starting from the given timestamp (page index)
            List<Order> orders = orderService.getFiftyOrders(barID, startingInstant, index);
            // Order from most recent to least recent (if not already ordered)
            orders.sort((o1, o2) -> o2.getTimestamp().compareTo(o1.getTimestamp()));

            return ResponseEntity.ok("{\"orders\":" + new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(orders) + "}");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing request");
        }
    }

    // Helper method for hashing (SHA-256)
    private String hash(String input) throws NoSuchAlgorithmException {
        String text = input + SECRET_KEY;
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(text.getBytes());
        return Base64.getEncoder().encodeToString(hash);
    }
}
