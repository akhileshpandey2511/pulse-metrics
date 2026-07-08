package com.pulse.metrics.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulse.metrics.config.KafkaConfig;
import com.pulse.metrics.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ReportRequestConsumer {

    private static final Logger log = LoggerFactory.getLogger(ReportRequestConsumer.class);

    private final ReportService reportService;
    private final ObjectMapper objectMapper;

    public ReportRequestConsumer(ReportService reportService, ObjectMapper objectMapper) {
        this.reportService = reportService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaConfig.REPORTS_TOPIC, groupId = "pulse-reports-group")
    public void consumeReportRequest(String message) {
        try {
            JsonNode requestNode = objectMapper.readTree(message);
            String tenantId = requestNode.get("tenantId").asText();
            String eventType = requestNode.get("eventType").asText();
            String windowType = requestNode.get("windowType").asText();

            log.info("Received Kafka report request for tenant: {}", tenantId);
            reportService.generateCsvReport(tenantId, eventType, windowType);

        } catch (Exception e) {
            log.error("Failed to process report request message: {}", message, e);
        }
    }
}
