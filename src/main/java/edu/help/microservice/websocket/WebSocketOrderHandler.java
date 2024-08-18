package edu.help.microservice.websocket;

import java.io.IOException;
import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WebSocketOrderHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();
    private final DataSource dataSource; // Add this field

    // Constructor to initialize DataSource
    public WebSocketOrderHandler(DataSource dataSource) {
        this.dataSource = dataSource;
    }


    @Override
public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    String path = getPathParameter(session.getUri(), "barId", "userId", "orderId");
    System.out.println("Extracted path: " + path);
    
    if (path != null) {
        System.out.println("Adding session to sessionMap with path: " + path + ", session ID: " + session.getId());
        sessionMap.put(path, session);

        String message = fetchInitialStatus(path);
        if (message != null) {
            session.sendMessage(new TextMessage(message));
            System.out.println("Sent initial status message: " + message);
        } else {
            // Handle the case where no result was found
            String noResultMessage = "No status found for path: " + path;
            session.sendMessage(new TextMessage(noResultMessage));
            System.out.println("Sent no result message to client: " + noResultMessage);
        }
    }
}


    private String getPathParameter(URI uri, String barParam, String userParam, String orderParam) {
        if (uri == null || uri.getQuery() == null) {
            return null;
        }
        
        Map<String, String> params = List.of(uri.getQuery().split("&")).stream()
            .map(s -> s.split("="))
            .filter(s -> s.length > 1)
            .collect(Collectors.toMap(s -> s[0], s -> s[1]));
        
        String barId = params.get(barParam);
        String userId = params.get(userParam);
        String orderId = params.get(orderParam);

        if (barId != null && userId != null && orderId != null) {
            return "root." + barId + "." + userId + "." + orderId.replace("-", "_");
        }
        return null;
    }

    public void sendOrderUpdate(String path, String message) {
        System.out.println("Looking for session with path: " + path);

        WebSocketSession session = sessionMap.get(path);
        if (session != null && session.isOpen()) {
            try {
                System.out.println("Sending update to path: " + path + " with message: " + message);
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("No open session found for path: " + path);
        }
    }
    
    
    private String fetchInitialStatus(String path) {
        // Extract barId, userId, and orderId from the path
        String[] parts = path.split("\\.");
    
        if (parts.length >= 4) {
            String barId = parts[1];
            String userId = parts[2];
            String orderId = parts[3].replace("-", "_"); // Replace dashes with underscores
    
            System.out.println("Debug: Extracted barId: " + barId + ", userId: " + userId + ", orderId: " + orderId);
    
            try (Connection connection = dataSource.getConnection();
                 Statement stmt = connection.createStatement()) {
    
                String query = String.format(
                    "SELECT path, status, claimer FROM hierarchy WHERE path ~ 'root.%s.%s.%s.*.*'",
                    barId, userId, orderId);
                System.out.println("Debug: Executing query: " + query);
    
                ResultSet rs = stmt.executeQuery(query);
                Map<String, Integer> drinkQuantities = new ConcurrentHashMap<>();
                String status = null;
                String claimer = null;
    
                while (rs.next()) {
                    String fullPath = rs.getString("path");
    
                    if (status == null) {
                        status = rs.getString("status"); // Assuming status is the same for all drinks in the order
                    }
    
                    if (claimer == null) {
                        claimer = rs.getString("claimer"); // Fetch claimer if available
                    }
    
                    String[] pathParts = fullPath.split("\\.");
                    if (pathParts.length == 6) {
                        String drinkId = pathParts[4]; // Extract drink ID
                        int drinkCount = Integer.parseInt(pathParts[5]); // Extract drink count
                        drinkQuantities.put(drinkId, drinkCount);
                        System.out.println("Debug: Added to drinkQuantities -> Drink ID: " + drinkId + ", Count: " + drinkCount);
                    } else {
                        System.out.println("Debug: Unexpected path length: " + fullPath);
                    }
                }
    
                if (!drinkQuantities.isEmpty()) {
                    Map<String, Object> result = new ConcurrentHashMap<>();
                    result.put("barId", Integer.valueOf(barId));
                    result.put("userId", Integer.valueOf(userId));
                    result.put("orderId", orderId);
    
                    // Use default values if status or claimer is null
                    result.put("status", status != null ? status : "unready");
                    result.put("claimer", claimer != null ? claimer : "unclaimed");
    
                    result.put("drinkQuantities", drinkQuantities);
    
                    // Convert the map to a JSON string
                    return new ObjectMapper().writeValueAsString(result);
                } else {
                    System.out.println("Debug: drinkQuantities map is empty despite matching paths.");
                }
            } catch (SQLException | JsonProcessingException e) {
                e.printStackTrace();
                System.out.println("Debug: Exception encountered: " + e.getMessage());
            }
        } else {
            System.out.println("Debug: Path does not contain enough parts: " + path);
        }
        return null;
    }
    
    



    
    
}
