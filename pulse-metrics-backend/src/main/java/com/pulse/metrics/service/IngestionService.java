package com.pulse.metrics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulse.metrics.config.KafkaConfig;
import com.pulse.metrics.model.MetricEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public IngestionService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void ingest(MetricEvent event) {
        try {
            if (event.getTimestamp() == null) {
                event.setTimestamp(System.currentTimeMillis());
            }
            String jsonEvent = objectMapper.writeValueAsString(event);
            log.debug("Ingesting event to Kafka: {}", jsonEvent);
            kafkaTemplate.send(KafkaConfig.EVENTS_TOPIC, event.getTenantId(), jsonEvent);
        } catch (Exception e) {
            log.error("Failed to ingest event: {}", event, e);
            throw new RuntimeException("Ingestion failed", e);
        }
    }
}
