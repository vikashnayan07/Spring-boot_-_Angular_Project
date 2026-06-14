package com.tcs.Machcare.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.Machcare.dto.RealtimeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
public class RealtimeEventService {
    private static final Logger log = LoggerFactory.getLogger(RealtimeEventService.class);
    private static final long SSE_TIMEOUT_MS = 30L * 60L * 1000L;

    private final Map<Long, List<SseEmitter>> userEmitters = new ConcurrentHashMap<>();
    private final Map<Integer, List<SseEmitter>> roleEmitters = new ConcurrentHashMap<>();
    private final Map<Long, List<WebSocketSession>> userSockets = new ConcurrentHashMap<>();
    private final Map<Integer, List<WebSocketSession>> roleSockets = new ConcurrentHashMap<>();
    private final Map<String, SocketRegistration> socketRegistrations = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ObjectMapper objectMapper;

    public RealtimeEventService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SseEmitter connect(Long empId, Integer roleId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        userEmitters.computeIfAbsent(empId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        roleEmitters.computeIfAbsent(roleId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        log.info("SSE connected empId={} roleId={} roleConnections={}", empId, roleId, connectionCountForRole(roleId));
        ScheduledFuture<?> heartbeat = heartbeatExecutor.scheduleAtFixedRate(
                () -> sendToEmitter(emitter, new RealtimeEvent("heartbeat", Map.of("empId", empId))),
                20,
                20,
                TimeUnit.SECONDS
        );

        Runnable cleanup = () -> {
            heartbeat.cancel(true);
            remove(userEmitters, empId, emitter);
            remove(roleEmitters, roleId, emitter);
        };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(error -> cleanup.run());

        sendToEmitter(emitter, new RealtimeEvent("connected", Map.of("empId", empId, "roleId", roleId)));
        return emitter;
    }

    public void connectWebSocket(Long empId, Integer roleId, WebSocketSession session) {
        userSockets.computeIfAbsent(empId, ignored -> new CopyOnWriteArrayList<>()).add(session);
        roleSockets.computeIfAbsent(roleId, ignored -> new CopyOnWriteArrayList<>()).add(session);
        socketRegistrations.put(session.getId(), new SocketRegistration(empId, roleId));
        log.info("WebSocket connected empId={} roleId={} roleConnections={}", empId, roleId, socketCountForRole(roleId));
        sendToSocket(session, new RealtimeEvent("connected", Map.of("empId", empId, "roleId", roleId, "transport", "websocket")));
    }

    public void disconnectWebSocket(WebSocketSession session) {
        SocketRegistration registration = socketRegistrations.remove(session.getId());
        if (registration == null) {
            return;
        }
        removeSocket(userSockets, registration.empId(), session);
        removeSocket(roleSockets, registration.roleId(), session);
        log.info("WebSocket disconnected empId={} roleId={}", registration.empId(), registration.roleId());
    }

    public void emitToUser(Long empId, String type, Object payload) {
        emitAfterCommit(() -> {
            RealtimeEvent event = new RealtimeEvent(type, payload);
            emit(userEmitters.get(empId), event);
            emitSockets(userSockets.get(empId), event);
        });
    }

    public void emitToRole(Integer roleId, String type, Object payload) {
        emitAfterCommit(() -> {
            RealtimeEvent event = new RealtimeEvent(type, payload);
            emit(roleEmitters.get(roleId), event);
            emitSockets(roleSockets.get(roleId), event);
        });
    }

    public void emitToEveryone(String type, Object payload) {
        emitAfterCommit(() -> {
            RealtimeEvent event = new RealtimeEvent(type, payload);
            roleEmitters.keySet().forEach(roleId -> emit(roleEmitters.get(roleId), event));
            roleSockets.keySet().forEach(roleId -> emitSockets(roleSockets.get(roleId), event));
        });
    }

    private void emitAfterCommit(Runnable emitterAction) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            emitterAction.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                emitterAction.run();
            }
        });
    }

    private void emit(List<SseEmitter> emitters, RealtimeEvent event) {
        if (emitters == null || emitters.isEmpty()) {
            log.info("SSE event skipped type={} because no active subscribers", event.getType());
            return;
        }
        log.info("SSE emitting type={} subscribers={}", event.getType(), emitters.size());
        emitters.forEach(emitter -> sendToEmitter(emitter, event));
    }

    private void emitSockets(List<WebSocketSession> sockets, RealtimeEvent event) {
        if (sockets == null || sockets.isEmpty()) {
            log.info("WebSocket event skipped type={} because no active subscribers", event.getType());
            return;
        }
        log.info("WebSocket emitting type={} subscribers={}", event.getType(), sockets.size());
        sockets.forEach(session -> sendToSocket(session, event));
    }

    private void sendToEmitter(SseEmitter emitter, RealtimeEvent event) {
        try {
            emitter.send(SseEmitter.event().name(event.getType()).data(event));
        } catch (IOException | RuntimeException ex) {
            log.warn("SSE send failed for type={}: {}", event.getType(), ex.getMessage());
            emitter.completeWithError(ex);
        }
    }

    private void sendToSocket(WebSocketSession session, RealtimeEvent event) {
        if (!session.isOpen()) {
            disconnectWebSocket(session);
            return;
        }
        synchronized (session) {
            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(event)));
            } catch (IOException | RuntimeException ex) {
                log.warn("WebSocket send failed for type={}: {}", event.getType(), ex.getMessage());
                disconnectWebSocket(session);
            }
        }
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> snapshot = new ConcurrentHashMap<>();
        userEmitters.forEach((empId, emitters) -> snapshot.put("user:" + empId, emitters.size()));
        roleEmitters.forEach((roleId, emitters) -> snapshot.put("role:" + roleId, emitters.size()));
        userSockets.forEach((empId, sockets) -> snapshot.put("ws-user:" + empId, sockets.size()));
        roleSockets.forEach((roleId, sockets) -> snapshot.put("ws-role:" + roleId, sockets.size()));
        return snapshot;
    }

    private int connectionCountForRole(Integer roleId) {
        List<SseEmitter> emitters = roleEmitters.get(roleId);
        return emitters == null ? 0 : emitters.size();
    }

    private int socketCountForRole(Integer roleId) {
        List<WebSocketSession> sockets = roleSockets.get(roleId);
        return sockets == null ? 0 : sockets.size();
    }

    private <T> void remove(Map<T, List<SseEmitter>> registry, T key, SseEmitter emitter) {
        List<SseEmitter> emitters = registry.get(key);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            registry.remove(key);
        }
    }

    private <T> void removeSocket(Map<T, List<WebSocketSession>> registry, T key, WebSocketSession session) {
        List<WebSocketSession> sessions = registry.get(key);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            registry.remove(key);
        }
    }

    private record SocketRegistration(Long empId, Integer roleId) {}
}
