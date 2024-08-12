package edu.help.microservice.config;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import edu.help.microservice.websocket.WebSocketOrderHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final DataSource dataSource;

    public WebSocketConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }


    @SuppressWarnings("null")
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketOrderHandler(), "/ws/hierarchy")
                .setAllowedOrigins("*")  // Set allowed origins if needed
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }

    @Bean
    public WebSocketOrderHandler webSocketOrderHandler() {
        return new WebSocketOrderHandler(dataSource);
    }
}
