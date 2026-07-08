package com.pulse.metrics.service;

import com.pulse.metrics.model.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AlertStreamService {

    private static final Logger log = LoggerFactory.getLogger(AlertStreamService.class);
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String tenantId) {
        SseEmitter emitter = new SseEmitter(24 * 60 * 60 * 1000L); // 24 hours timeout
        emitters.computeIfAbsent(tenantId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(tenantId, emitter));
        emitter.onTimeout(() -> removeEmitter(tenantId, emitter));
        emitter.onError((e) -> removeEmitter(tenantId, emitter));

        // Send dummy connection event
        try {
            emitter.send(SseEmitter.event().name("connect").data("Connected to live alert stream"));
        } catch (IOException e) {
            removeEmitter(tenantId, emitter);
        }

        return emitter;
    }

    public void sendAlert(String tenantId, Alert alert) {
        List<SseEmitter> tenantEmitters = emitters.get(tenantId);
        if (tenantEmitters == null || tenantEmitters.isEmpty()) return;

        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
        for (SseEmitter emitter : tenantEmitters) {
            try {
                emitter.send(SseEmitter.event().name("alert").data(alert));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }
        tenantEmitters.removeAll(deadEmitters);
    }

    private void removeEmitter(String tenantId, SseEmitter emitter) {
        List<SseEmitter> tenantEmitters = emitters.get(tenantId);
        if (tenantEmitters != null) {
            tenantEmitters.remove(emitter);
        }
    }
}
