package edu.help.microservice.service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import edu.help.microservice.websocket.WebSocketOrderHandler;
=
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
public class OrderStatusService {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private WebSocketOrderHandler webSocketOrderHandler;

    private Connection connection;
    
    @PostConstruct
    public void startListening() {
        try {
            connection = dataSource.getConnection();
            PGConnection pgConnection = connection.unwrap(PGConnection.class);
            Statement stmt = connection.createStatement();
            stmt.execute("LISTEN order_status_changed");
            stmt.close();

            new Thread(() -> {
                while (true) {
                    try (Statement pollingStatement = connection.createStatement()) {
                        ResultSet rs = pollingStatement.executeQuery("SELECT 1"); // Simple query to trigger notification processing
                        rs.close();

                        PGNotification[] notifications = pgConnection.getNotifications();
                        if (notifications != null) {
                            for (PGNotification notification : notifications) {
                                String payload = notification.getParameter();
                                String[] parts = payload.split("\\.");
                                if (parts.length >= 3) {
                                    String barId = parts[1];
                                    String userId = parts[2];
                                    String orderId = parts[3];
                                    String path = "root." + barId + "." + userId + "." + orderId;

                                    String query = String.format(
                                        "SELECT status, claimer FROM hierarchy WHERE bar_id = '%s' AND user_id = '%s' AND order_id = '%s'",
                                            barId, userId, orderId);

                                    Statement statusStatement = connection.createStatement();
                                    ResultSet statusResult = statusStatement.executeQuery(query);
                                    if (statusResult.next()) {
                                        String status = statusResult.getString("status");
                                        String claimer = statusResult.getString("claimer");
                                        String statusMessage = (status != null) ? status : "null";
                                        String claimerMessage = (claimer != null) ? claimer : "null";

                                        // Include both status and claimer in the message
                                        String message = String.format("Path: %s, Status: %s, Claimer: %s", path, statusMessage, claimerMessage);
                                        webSocketOrderHandler.sendOrderUpdate(path, message);
                                    }
                                    statusResult.close();
                                    statusStatement.close();
                                }
                            }
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void stopListening() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
