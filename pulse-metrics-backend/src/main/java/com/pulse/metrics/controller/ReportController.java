package com.pulse.metrics.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulse.metrics.config.KafkaConfig;
import com.pulse.metrics.config.TenantContext;
import com.pulse.metrics.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ReportService reportService;
    private final ObjectMapper objectMapper;

    public ReportController(KafkaTemplate<String, String> kafkaTemplate,
                            ReportService reportService,
                            ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.reportService = reportService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/request")
    public ResponseEntity<Map<String, String>> requestReport(@RequestBody Map<String, String> requestParams) {
        String eventType = requestParams.get("eventType");
        String windowType = requestParams.get("windowType");

        if (eventType == null || windowType == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "eventType and windowType are required"));
        }

        String tenantId = TenantContext.getCurrentTenant();

        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("tenantId", tenantId);
            payload.put("eventType", eventType);
            payload.put("windowType", windowType);

            String message = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(KafkaConfig.REPORTS_TOPIC, tenantId, message);
            log.info("Published report request to Kafka topic for tenant {}", tenantId);

            return ResponseEntity.ok(Map.of("message", "Report request submitted successfully. Processing via Kafka consumer."));
        } catch (Exception e) {
            log.error("Failed to request report via Kafka", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to submit report request"));
        }
    }

    @GetMapping("/download/{filename}")
    public ResponseEntity<Resource> downloadReport(@PathVariable String filename) {
        // Enforce basic directory traversal protection
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return ResponseEntity.badRequest().build();
        }

        File file = new File("./reports/" + filename);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .body(resource);
    }
}
