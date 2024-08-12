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
        // Replace dashes with underscores in the orderId
        String orderId = parts[3].replace("-", "_");

        System.out.println("Debug: Extracted barId: " + barId + ", userId: " + userId + ", orderId: " + orderId);

        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement()) {

            // Match paths with the first 4 levels and collect all drink IDs
            String query = String.format(
                "SELECT path, status FROM hierarchy WHERE path ~ 'root.%s.%s.%s.*' ORDER BY id ASC",
                barId, userId, orderId);
            System.out.println("Debug: Executing query: " + query);

            ResultSet rs = stmt.executeQuery(query);
            StringBuilder drinkIds = new StringBuilder();
            int status = -1;

            while (rs.next()) {
                String fullPath = rs.getString("path");
                if (status == -1) {
                    status = rs.getInt("status"); // Assuming status is the same for all drinks in the order
                }
                String[] pathParts = fullPath.split("\\.");
                if (pathParts.length == 5) {
                    drinkIds.append(pathParts[4]).append(", ");
                }
                System.out.println("Debug: Query returned path: " + fullPath);
            }

            if (drinkIds.length() > 0) {
                // Remove the last comma and space
                drinkIds.setLength(drinkIds.length() - 2);
                return String.format("root.%s.%s.%s.{%s}, Status: %d", barId, userId, orderId, drinkIds.toString(), status);
            } else {
                System.out.println("Debug: No results found for path: " + path);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Debug: SQLException encountered: " + e.getMessage());
        }
    } else {
        System.out.println("Debug: Path does not contain enough parts: " + path);
    }
    return null;
}


    
    
}
