package com.tcs.Machcare.service;

import com.tcs.Machcare.util.Jwtutil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class RealtimeWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(RealtimeWebSocketHandler.class);

    private final RealtimeEventService realtimeEventService;
    private final Jwtutil jwtUtil;

    public RealtimeWebSocketHandler(RealtimeEventService realtimeEventService, Jwtutil jwtUtil) {
        this.realtimeEventService = realtimeEventService;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = UriComponentsBuilder.fromUri(session.getUri())
                .build()
                .getQueryParams()
                .getFirst("token");

        if (token == null || token.isBlank()) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Missing token"));
            return;
        }

        try {
            Long empId = jwtUtil.extractEmpId(token);
            Integer roleId = jwtUtil.extractRoleId(token);
            realtimeEventService.connectWebSocket(empId, roleId, session);
        } catch (RuntimeException ex) {
            log.warn("WebSocket authentication failed: {}", ex.getMessage());
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Invalid token"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        realtimeEventService.disconnectWebSocket(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.warn("WebSocket transport error: {}", exception.getMessage());
        realtimeEventService.disconnectWebSocket(session);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }
}
