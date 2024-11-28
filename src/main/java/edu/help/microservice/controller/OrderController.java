package edu.help.microservice.controller;

import edu.help.microservice.dto.OrderDTO;
import edu.help.microservice.dto.TipClaimRequest;
import edu.help.microservice.entity.Order;
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

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.List;
import java.util.Properties;

import edu.help.microservice.entity.Bar;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final SignUpService signUpService;
    private final BarService barService; 

    @Autowired
    public OrderController(OrderService orderService, SignUpService signUpService, BarService barService) {
        this.orderService = orderService;
        this.signUpService = signUpService;
        this.barService = barService;
    }

    // Endpoint to save an order
    @PostMapping("/save")
    public ResponseEntity<String> saveOrder(@RequestBody OrderDTO order) {
        orderService.saveOrder(order);
        return ResponseEntity.ok("Order saved successfully");
    }

    @PostMapping("/claim")
    public ResponseEntity<Double> claimTips(@RequestBody TipClaimRequest request) {
        try {
            int barID = request.getBarId();
            String bartenderName = request.getBartenderName();
            String bartenderEmail = request.getBartenderEmail(); // Optional
            String station = request.getStation();

            System.out.println("Request received with barID: " + barID + ", bartenderName: " + bartenderName 
            + ", bartenderEmail: " + bartenderEmail + ", station: " + station);

            // Fetch unclaimed orders
            List<Order> tipsList = orderService.getUnclaimedTips(barID, station);

            if (tipsList.isEmpty()) {
                // Return -1 if no unclaimed tips are found
                return ResponseEntity.ok(-1.0);
            }

            // Update orders to set tipsClaimed to bartender's name
            orderService.claimTipsForOrders(tipsList, bartenderName);
            System.out.println("Updated orders with bartender's name: " + bartenderName);

            // Calculate total tip amount
            double totalTipAmount = calculateTotalTipAmount(tipsList);
            System.out.println("Total tip amount calculated: " + totalTipAmount);

            // Retrieve bar email
            String barEmail = signUpService.findEmailByBarId(barID);
            if (barEmail == null || barEmail.isEmpty()) {
                System.err.println("Bar email not found for bar ID: " + barID);
            }

            // Prepare email content
            String emailContent = prepareEmailContent(barID, bartenderName, bartenderEmail, station, tipsList);

            // Send emails
            String subject = "Tip Receipt for " + bartenderName + " at " + barService.findBarById(barID).getBarName();
            if (barEmail != null && !barEmail.isEmpty()) {
                sendTipEmail(barEmail, subject, emailContent);
            }
            if (bartenderEmail != null && !bartenderEmail.isEmpty()) {
                sendTipEmail(bartenderEmail, subject, emailContent);
            }

            System.out.println("ClaimTips process completed successfully. Total tips: " + totalTipAmount);


            // Return total tip amount to the frontend
            return ResponseEntity.ok(totalTipAmount);

        } catch (Exception e) {
            System.err.println("Error occurred in claimTips process: " + e.getMessage());
            e.printStackTrace();
            // Return -1 if an error occurs
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

    private String prepareEmailContent(int barID, String bartenderName, String bartenderEmail, String station, List<Order> tipsList) throws Exception {
        // Get current date and format it
        ZonedDateTime now = ZonedDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a z");
        String formattedDate = now.format(formatter);

        // Build email content
        StringBuilder emailContent = new StringBuilder();
        emailContent.append("<h1 style='text-align:center;'>Tip Report</h1>")
                .append("<h2 style='text-align:center;'>Bar ID: ").append(barID).append("</h2>")
                .append("<p style='text-align:center; font-weight:bold;'>Bartender Station: <span style='color:blue;'>")
                .append(station).append("</span> | Bartender Name: <span style='color:blue;'>")
                .append(bartenderName).append("</span></p>")
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
            helper.setFrom("noreply@barzzy.site");
            helper.setSubject(subject);

            // Set the email content as HTML
            helper.setText(content, true);

            // Send the email
            test.send(message);
            System.out.println("Email successfully sent to " + email);
        } catch (Exception ex) {
            System.err.println("Error sending email to " + email);
            ex.printStackTrace();
        }
    }
}
