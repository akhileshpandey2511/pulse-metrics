package com.pulse.metrics.controller;

import com.pulse.metrics.config.TenantContext;
import com.pulse.metrics.model.MetricEvent;
import com.pulse.metrics.service.IngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final IngestionService ingestionService;

    public EventController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping
    public ResponseEntity<String> ingestEvent(@RequestBody MetricEvent event) {
        // Enforce tenant from the current ThreadLocal context (established by TenantFilter)
        event.setTenantId(TenantContext.getCurrentTenant());
        ingestionService.ingest(event);
        return ResponseEntity.ok("Event ingested successfully");
    }
}
