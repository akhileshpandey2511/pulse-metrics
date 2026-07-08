package com.pulse.metrics.controller;

import com.pulse.metrics.config.TenantContext;
import com.pulse.metrics.model.Alert;
import com.pulse.metrics.repository.AlertRepository;
import com.pulse.metrics.service.AlertStreamService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertRepository alertRepository;
    private final AlertStreamService alertStreamService;

    public AlertController(AlertRepository alertRepository, AlertStreamService alertStreamService) {
        this.alertRepository = alertRepository;
        this.alertStreamService = alertStreamService;
    }

    @GetMapping
    public ResponseEntity<List<Alert>> getAlerts(@RequestParam(required = false) String status) {
        String tenantId = TenantContext.getCurrentTenant();
        List<Alert> alerts;
        if (status != null && !status.isEmpty()) {
            alerts = alertRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, status);
        } else {
            alerts = alertRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        }
        return ResponseEntity.ok(alerts);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAlerts() {
        String tenantId = TenantContext.getCurrentTenant();
        return alertStreamService.subscribe(tenantId);
    }
}
