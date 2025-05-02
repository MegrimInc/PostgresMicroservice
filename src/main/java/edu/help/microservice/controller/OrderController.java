package edu.help.microservice.controller;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.help.microservice.dto.GetTipsResponse;
import edu.help.microservice.dto.OrderDTO;
import edu.help.microservice.dto.OrderRequest;
import edu.help.microservice.dto.OrderResponse;
import edu.help.microservice.dto.TipClaimRequest;
import edu.help.microservice.entity.Order;
import edu.help.microservice.service.MerchantService;
import edu.help.microservice.service.OrderService;
import edu.help.microservice.service.AuthService;
import jakarta.mail.internet.MimeMessage;

@RestController
@RequestMapping("/postgres-test/order")
public class OrderController {

    private final OrderService orderService;
    private final AuthService signUpService;
    private final MerchantService merchantService;

    @Autowired
    public OrderController(OrderService orderService, AuthService signUpService, MerchantService merchantService) {
        this.orderService = orderService;
        this.signUpService = signUpService;
        this.merchantService = merchantService;
    }

    // Endpoint to save an order
    @PostMapping("/save")
    public ResponseEntity<String> saveOrder(@RequestBody OrderDTO order) {
        orderService.saveOrder(order);
        return ResponseEntity.ok("Order saved successfully");
    }


@PostMapping("/{merchantId}/processOrder")
    public ResponseEntity<OrderResponse> processOrder(@PathVariable int merchantId, @RequestBody OrderRequest orderRequest) {
        // Delegate processing to the service layer
        OrderResponse response = merchantService.processOrder(merchantId, orderRequest);
        // Return the processed order response
        return ResponseEntity.ok(response);
    }

    /**
     * New endpoint to retrieve the total tips claimed by a station.
     * It accepts a JSON payload of the form:
     *     { "terminalId": "A" }
     * and returns a JSON response of the form:
     *     { "tipTotal": 1.23 }
     */
    @GetMapping("/gettips")
    public ResponseEntity<GetTipsResponse> getTips(
            @RequestParam("terminalId") String terminalId,
            @RequestParam("merchantId") String merchantIdStr) {
        System.out.println("gettips");

        // Convert merchantID from String to int
        int merchantID;
        try {
            merchantID = Integer.parseInt(merchantIdStr);
        } catch (NumberFormatException e) {
            System.err.println("Invalid merchantID provided: " + merchantIdStr);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new GetTipsResponse(-1.0));
        }

        System.out.println("gettips for " + terminalId + " " + merchantID);
        // Validate that the stationID is a single uppercase letter A-Z.
        if (terminalId == null || !terminalId.matches("^[A-Z]$")) {
            System.err.println("Invalid stationID provided: " + terminalId);
            // Here we return a 400 Bad Request. Alternatively, you could return a specific error tipTotal (e.g., -2.0).
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new GetTipsResponse(-1.0));
        }
        try {
            // Retrieve orders that have been claimed by this station.
            // (Assumes that orderService has a method like getClaimedTipsByStation.)
            List<Order> claimedOrders = orderService.getUnclaimedTips(merchantID, terminalId);
            if (claimedOrders == null || claimedOrders.isEmpty()) {
            System.out.println("gettips NULL / empty");
                return ResponseEntity.ok(new GetTipsResponse(0.0));
            }

            // Filter out orders that did not use in-app payments (same logic as in claimTips).
            claimedOrders.removeIf(order -> !order.isInAppPayments());

            // Calculate the total tip amount.
            double totalTipAmount = calculateTotalTipAmount(claimedOrders);
            System.out.println("Total tip amount for station " + terminalId + ": " + totalTipAmount);

            return ResponseEntity.ok(new GetTipsResponse(totalTipAmount));
        } catch (Exception e) {
            System.err.println("Error in getTips: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new GetTipsResponse(-2.0));
        }
    }

    @PostMapping("/claim")
    public ResponseEntity<Double> claimTips(@RequestBody TipClaimRequest request) {
        try {
            int merchantID = request.getMerchantId();
            String stationName = request.getStationName();
            String stationEmail = request.getStationEmail(); // Optional
            String station = request.getStation();

            System.out.println("Request received with merchantID: " + merchantID + ", stationName: " + stationName
                    + ", stationEmail: " + stationEmail + ", station: " + station);

            // Fetch unclaimed orders
            List<Order> tipsList = orderService.getUnclaimedTips(merchantID, station);

            // Filter out orders where inAppPayments is false
            tipsList.removeIf(order -> !order.isInAppPayments());

            if (tipsList.isEmpty()) {
                // Return -1 if no unclaimed tips are found
                return ResponseEntity.ok(-1.0);
            }

            // Update orders to set tipsClaimed to station's name
            orderService.claimTipsForOrders(tipsList, stationName);
            System.out.println("Updated orders with station's name: " + stationName);

            // Calculate total tip amount
            double totalTipAmount = calculateTotalTipAmount(tipsList);
            System.out.println("Total tip amount calculated: " + totalTipAmount);

            // Retrieve merchant email
            String merchantEmail = signUpService.findEmailByMerchantId(merchantID);
            if (merchantEmail == null || merchantEmail.isEmpty()) {
                System.err.println("Merchant email not found for merchant ID: " + merchantID);
            }

            // Prepare email content
            String emailContent = prepareEmailContent(merchantID, stationName, stationEmail, station, tipsList);

            // Send emails
            String subject = "Tip Receipt for " + stationName + " at " + merchantService.findMerchantById(merchantID).getName();
            if (merchantEmail != null && !merchantEmail.isEmpty()) {
                sendTipEmail(merchantEmail, subject, emailContent);
            }
            if (stationEmail != null && !stationEmail.isEmpty()) {
                sendTipEmail(stationEmail, subject, emailContent);
            }

            System.out.println("ClaimTips process completed successfully. Total tips: " + totalTipAmount);

            // Return total tip amount to the frontend
            return ResponseEntity.ok(totalTipAmount);

        } catch (Exception e) {
            System.err.println("Error occurred in claimTips process: " + e.getMessage());
            e.printStackTrace();
            // Return -2 if an error occurs
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(-2.0);
        }
    }


    
    // Helper methods...

    private double calculateTotalTipAmount(List<Order> tipsList) {
        double totalTipAmount = 0.0;
        for (Order order : tipsList) {
            totalTipAmount += order.getTip();
        }
        return totalTipAmount;
    }

    private String prepareEmailContent(int merchantID, String stationName, String stationEmail, String station, List<Order> tipsList) throws Exception {
        // Get current date and format it
        ZonedDateTime now = ZonedDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a z");
        String formattedDate = now.format(formatter);

        // Build email content
        StringBuilder emailContent = new StringBuilder();
        emailContent.append("<h1 style='text-align:center;'>Tip Report</h1>")
                .append("<h2 style='text-align:center;'>Merchant Name: ").append(merchantService.findMerchantById(merchantID).getName()).append("</h2>")
                .append("<p style='text-align:center; font-weight:bold;'>Station Station: <span style='color:blue;'>")
                .append(station).append("</span> | Station Name: <span style='color:blue;'>")
                .append(stationName).append("</span></p>")
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
                    .append(": $").append(String.format("%.2f", order.getTip()))
                    .append(" | ").append(orderFormattedDate).append("</li>");
        }
        emailContent.append("</ul>");

        return emailContent.toString();
    }


    // Use your existing sendTipEmail method
    private void sendTipEmail(String email, String subject, String content) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("email-smtp.us-east-1.amazonaws.com");
        mailSender.setPort(587);

        mailSender.setUsername("YOUR_SMTP_USERNAME");
        mailSender.setPassword("YOUR_SMTP_PASSWORD");

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true");

        try {
            // Create a MimeMessage
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            // Set the basic email attributes
            helper.setTo(email);
            helper.setFrom("noreply@barzzy.site"); // SHOULD BE RENAMED TO MEGRIM LATER
            helper.setSubject(subject);

            // Set the email content as HTML
            helper.setText(content, true);

            // Send the email
            mailSender.send(message);
            System.out.println("Email successfully sent to " + email);
        } catch (Exception ex) {
            System.err.println("Error sending email to " + email);
            ex.printStackTrace();
        }
    }
}