package com.pulse.metrics.controller;

import com.pulse.metrics.config.TenantContext;
import com.pulse.metrics.model.MetricAggregated;
import com.pulse.metrics.repository.MetricAggregatedRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/metrics")
public class MetricsController {

    private final MetricAggregatedRepository metricRepository;

    public MetricsController(MetricAggregatedRepository metricRepository) {
        this.metricRepository = metricRepository;
    }

    @GetMapping("/aggregated")
    public ResponseEntity<List<MetricAggregated>> getAggregatedMetrics(
            @RequestParam String eventType,
            @RequestParam(defaultValue = "1m") String windowType,
            @RequestParam(required = false) Integer limitHours) {

        String tenantId = TenantContext.getCurrentTenant();
        int hours = limitHours != null ? limitHours : 1;
        LocalDateTime start = LocalDateTime.now().minusHours(hours);

        List<MetricAggregated> metrics = metricRepository
                .findByTenantIdAndWindowTypeAndEventTypeAndWindowStartAfterOrderByWindowStartAsc(
                        tenantId, windowType, eventType, start);

        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/types")
    public ResponseEntity<List<String>> getEventTypes() {
        String tenantId = TenantContext.getCurrentTenant();
        List<String> eventTypes = metricRepository.findDistinctEventTypesByTenantId(tenantId);
        return ResponseEntity.ok(eventTypes);
    }
}
