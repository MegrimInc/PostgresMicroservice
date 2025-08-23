package edu.help.microservice.controller;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import edu.help.microservice.entity.Auth;
import edu.help.microservice.entity.Employee;
import edu.help.microservice.entity.Merchant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.help.microservice.dto.EmployeeShiftSummary;
import edu.help.microservice.dto.InventoryResponse;
import edu.help.microservice.dto.MerchantDTO;
import edu.help.microservice.dto.OrderDTO;
import edu.help.microservice.dto.OrderRequest;
import edu.help.microservice.dto.OrderResponse;
import edu.help.microservice.entity.Order;
import edu.help.microservice.repository.EmployeeRepository;
import edu.help.microservice.service.MerchantService;
import edu.help.microservice.service.OrderService;
import edu.help.microservice.service.S3Service;
import edu.help.microservice.util.DTOConverter;
import edu.help.microservice.service.AuthService;
import edu.help.microservice.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import static edu.help.microservice.config.SecurityConfig.HASH_KEY;

@RequiredArgsConstructor
@RestController
@RequestMapping("/employee")
public class EmployeeController {
    private final OrderService orderService;
    private final AuthService authService;
    private final MerchantService merchantService;
    private final S3Service s3Service;
    private final EmployeeRepository employeeRepository;
    private final EmployeeService employeeService;

    // Endpoint to save an order
    @PostMapping("/save")
    public ResponseEntity<String> saveOrder(@RequestBody OrderDTO order) {
        orderService.saveOrder(order);
        return ResponseEntity.ok("Order saved successfully");
    }

    @PostMapping("/{merchantId}/processOrder")
    public ResponseEntity<OrderResponse> processOrder(@PathVariable int merchantId,
            @RequestBody OrderRequest orderRequest) {

        System.out.println("[DEBUG] Starting processOrder");

        boolean valid = false;
        String password = orderRequest.getPassword();
        System.out.println("[DEBUG] Provided password: " + password);

        Optional<Auth> auth = authService.findByCustomerId(orderRequest.getCustomerId());
        Auth auth2 = null;
        if (auth.isPresent()) {
            auth2 = auth.get();
            System.out.println("[DEBUG] Auth record found for customer ID: " + orderRequest.getCustomerId());
        } else {
            System.out.println("[DEBUG] No Auth record found for customer ID: " + orderRequest.getCustomerId());
        }

        if (auth2 != null) {
            try {
                String hashedPassword = hash(password);
                System.out.println("[DEBUG] Hashed password: " + hashedPassword);
                System.out.println("[DEBUG] Stored passcode: " + auth2.getPasscode());

                if (auth2.getPasscode() != null && auth2.getPasscode().equals(hashedPassword)) {
                    System.out.println("[DEBUG] Password match");
                    if (auth2.getCustomer() != null) {
                        System.out.println("[DEBUG] Customer object is linked");
                        valid = true;
                    } else {
                        System.out.println("[DEBUG] Customer object is null");
                    }
                } else {
                    System.out.println("[DEBUG] Password mismatch");
                }
            } catch (NoSuchAlgorithmException e) {
                System.out.println("[DEBUG] Hashing failed: " + e.getMessage());
            }
        }

        if (!valid) {
            System.out.println("[DEBUG] Order validation failed. Returning error response.");
            return ResponseEntity.ok(OrderResponse.builder()
                    .message("Order processed unsuccessfully")
                    .messageType("error")
                    .totalGratuity(0.00)
                    .totalServiceFee(0.00)
                    .totalTax(0.00)
                    .inAppPayments(false)
                    .totalPrice(0.00)
                    .totalPointPrice(0)
                    .name("")
                    .items(new ArrayList<>())
                    .name(((Integer) orderRequest.getCustomerId()).toString())
                    .build());
        }

        System.out.println("[DEBUG] Order validation passed. Processing order...");
        OrderResponse response = orderService.processOrder(merchantId, orderRequest);
        return ResponseEntity.ok(response);
    }

    private String hash(String input) throws NoSuchAlgorithmException {
        String text = input + HASH_KEY;
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(text.getBytes());
        return Base64.getEncoder().encodeToString(hash);
    }

    // Helper methods...

    private double calculateTotalTipAmount(List<Order> tipsList) {
        double totalTipAmount = 0.0;
        for (Order order : tipsList) {
            totalTipAmount += order.getTotalGratuity();
        }
        return totalTipAmount;
    }

    private String prepareEmailContent(int merchantID, String claimer, String email, String station,
            List<Order> tipsList) throws Exception {
        // Get current date and format it
        ZonedDateTime now = ZonedDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a z");
        String formattedDate = now.format(formatter);

        // Build email content
        StringBuilder emailContent = new StringBuilder();
        emailContent.append("<h1 style='text-align:center;'>Tip Report</h1>")
                .append("<h2 style='text-align:center;'>Merchant Name: ")
                .append(merchantService.findMerchantById(merchantID).getName()).append("</h2>")
                .append("<p style='text-align:center; font-weight:bold;'>Station Station: <span style='color:blue;'>")
                .append(station).append("</span> | Station Name: <span style='color:blue;'>")
                .append(claimer).append("</span></p>")
                .append("<p style='text-align:center;'>Claimed at: <span style='color:green;'>")
                .append(formattedDate).append("</span></p>")
                .append("<h3 style='text-align:center; color:darkgreen;'>Total Tip Amount: <span style='color:green;'>$")
                .append(String.format("%.2f", calculateTotalTipAmount(tipsList))).append("</span></h3>");

        emailContent.append("<h3>Order Tips:</h3><ul>");
        for (Order order : tipsList) {
            Instant timestamp = order.getTimestamp();
            ZonedDateTime dateTime = timestamp.atZone(ZoneId.systemDefault()); // Convert Instant to ZonedDateTime
            String orderFormattedDate = dateTime.format(formatter);
            emailContent.append("<li><strong>Order ID#</strong> ").append(order.getOrderId())
                    .append(": $").append(String.format("%.2f", order.getTotalGratuity()))
                    .append(" | ").append(orderFormattedDate).append("</li>");
        }
        emailContent.append("</ul>");

        return emailContent.toString();
    }

    @GetMapping("/{merchantId}")
    public ResponseEntity<MerchantDTO> getMerchantById(@PathVariable Integer merchantId) {
        // 1) fetch the raw Merchant
        Merchant merchant = merchantService.findMerchantById(merchantId);
        if (merchant == null) {
            return ResponseEntity.notFound().build();
        }

        // 2) fetch its employees list
        List<Employee> employees = employeeRepository.findByMerchantId(merchantId);

        // 3) convert to DTO (includes JSON→Map parsing of discountSchedule)
        MerchantDTO dto = DTOConverter.convertToMerchantDTO(merchant, employees);

        // 4) return the DTO
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/upload-image-url")
    public ResponseEntity<Map<String, String>> getPresignedImageUploadUrl(
            @RequestParam Integer merchantId,
            @RequestParam String filename) {

        // build the S3 key under this merchant’s folder
        String key = merchantId + "/" + filename;

        PresignedPutObjectRequest presigned = s3Service.generatePresignedUrl(key); // no contentType now

        System.out.println("Presigned URL: " + presigned.url());
        System.out.println("Signed headers: " + presigned.signedHeaders()); // should show only {host=[…]}

        return ResponseEntity.ok(Map.of(
                "url", presigned.url().toString(),
                "key", java.net.URLEncoder.encode(key, java.nio.charset.StandardCharsets.UTF_8)));
    }

    @PostMapping("/createEmployee")
    public ResponseEntity<Employee> createEmployee(@RequestBody Employee employee) {
        employee.setShiftTimestamp(Instant.now());
        Employee saved = employeeRepository.save(employee);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @GetMapping("/getInventoryByMerchant/{merchantId}")
    public InventoryResponse getInventoryByMerchant(@PathVariable Integer merchantId) {
        return merchantService.getInventoryByMerchantId(merchantId);
    }

    @GetMapping("/{merchantId}/employees")
    public ResponseEntity<List<Employee>> getEmployeesByMerchantId(@PathVariable Integer merchantId) {
        List<Employee> employees = employeeRepository.findByMerchantId(merchantId);
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/{merchantId}/employee-shift-summaries")
    public ResponseEntity<List<EmployeeShiftSummary>> getAllEmployeeShiftSummary(@PathVariable int merchantId) {
        List<EmployeeShiftSummary> summaries = employeeService.getAllEmployeeShiftSummaries(merchantId);
        return ResponseEntity.ok(summaries);
    }
}