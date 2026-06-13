package com.tcs.Machcare.service;

import com.tcs.Machcare.dto.RealtimeEvent;
import org.springframework.stereotype.Service;
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
    private static final long SSE_TIMEOUT_MS = 30L * 60L * 1000L;

    private final Map<Long, List<SseEmitter>> userEmitters = new ConcurrentHashMap<>();
    private final Map<Integer, List<SseEmitter>> roleEmitters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();

    public SseEmitter connect(Long empId, Integer roleId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        userEmitters.computeIfAbsent(empId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        roleEmitters.computeIfAbsent(roleId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
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

    public void emitToUser(Long empId, String type, Object payload) {
        emit(userEmitters.get(empId), new RealtimeEvent(type, payload));
    }

    public void emitToRole(Integer roleId, String type, Object payload) {
        emit(roleEmitters.get(roleId), new RealtimeEvent(type, payload));
    }

    public void emitToEveryone(String type, Object payload) {
        roleEmitters.keySet().forEach(roleId -> emitToRole(roleId, type, payload));
    }

    private void emit(List<SseEmitter> emitters, RealtimeEvent event) {
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        emitters.forEach(emitter -> sendToEmitter(emitter, event));
    }

    private void sendToEmitter(SseEmitter emitter, RealtimeEvent event) {
        try {
            emitter.send(SseEmitter.event().name(event.getType()).data(event));
        } catch (IOException | IllegalStateException ex) {
            emitter.completeWithError(ex);
        }
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
}
