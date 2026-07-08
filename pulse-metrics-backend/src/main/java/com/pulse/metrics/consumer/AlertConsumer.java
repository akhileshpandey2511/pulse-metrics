package com.pulse.metrics.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulse.metrics.config.KafkaConfig;
import com.pulse.metrics.model.Alert;
import com.pulse.metrics.service.AlertStreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AlertConsumer {

    private static final Logger log = LoggerFactory.getLogger(AlertConsumer.class);

    private final AlertStreamService alertStreamService;
    private final ObjectMapper objectMapper;

    public AlertConsumer(AlertStreamService alertStreamService, ObjectMapper objectMapper) {
        this.alertStreamService = alertStreamService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaConfig.ANOMALIES_TOPIC, groupId = "pulse-alerts-group")
    public void consumeAnomaly(String message) {
        try {
            Alert alert = objectMapper.readValue(message, Alert.class);
            log.info("Consumed anomaly alert: {}", alert.getMessage());

            // Push to SSE clients
            alertStreamService.sendAlert(alert.getTenantId(), alert);

        } catch (Exception e) {
            log.error("Failed to process anomaly Kafka message: {}", message, e);
        }
    }
}
