package com.tcs.Machcare.config;

import com.tcs.Machcare.service.RealtimeWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class RealtimeWebSocketConfig implements WebSocketConfigurer {

    private final RealtimeWebSocketHandler realtimeWebSocketHandler;

    public RealtimeWebSocketConfig(RealtimeWebSocketHandler realtimeWebSocketHandler) {
        this.realtimeWebSocketHandler = realtimeWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(realtimeWebSocketHandler, "/api/realtime/ws")
                .setAllowedOrigins(
                        "https://machcare.me",
                        "https://www.machcare.me",
                        "http://machcare.me",
                        "http://www.machcare.me",
                        "http://localhost:4200",
                        "http://localhost:8080",
                        "http://localhost:9090"
                );
    }
}
