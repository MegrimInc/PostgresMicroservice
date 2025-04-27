package edu.help.microservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import edu.help.microservice.dto.ItemCountDTO;
import edu.help.microservice.entity.Order;
import edu.help.microservice.service.MerchantService;
import edu.help.microservice.service.OrderService;
import edu.help.microservice.service.SignUpService;
import edu.help.microservice.util.Cookies;
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

    private static final String SECRET_KEY = "YourSecretKey";

    @Autowired
    public MerchantController(OrderService orderService, SignUpService signUpService, MerchantService merchantService) {
        this.orderService = orderService;
    }

    @GetMapping("/generalData")
    public ResponseEntity<?> generalData(@CookieValue(value = "auth", required = false) String authCookie) {
        try {
            Integer merchantID = Cookies.getIdFromCookie(authCookie);
            if (merchantID <= -1) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid session. Please log in again.");
            }

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
            Integer merchantID = Cookies.getIdFromCookie(authCookie);
            if (merchantID <= -1) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid session. Please log in again.");
            }

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
            Integer merchantID = Cookies.getIdFromCookie(authCookie);
            if (merchantID <= -1) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid session. Please log in again.");
            }

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
            Integer merchantID = Cookies.getIdFromCookie(authCookie);
            if (merchantID <= -1) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid session. Please log in again.");
            }

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
            Integer merchantID = Cookies.getIdFromCookie(authCookie);
            if (merchantID <= -1) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid session. Please log in again.");
            }

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
            Integer merchantID = Cookies.getIdFromCookie(authCookie);
            if (merchantID <= -1) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid session. Please log in again.");
            }

            List<ItemCountDTO> responseList = orderService.getAllItemCountsForMerchant(merchantID);
            return ResponseEntity.ok(Map.of("data", responseList));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing request");
        }
    }

    
}
