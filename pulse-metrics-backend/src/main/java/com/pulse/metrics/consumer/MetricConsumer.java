package com.pulse.metrics.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulse.metrics.config.KafkaConfig;
import com.pulse.metrics.model.MetricEvent;
import com.pulse.metrics.service.AggregationService;
import com.pulse.metrics.service.AnomalyDetectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class MetricConsumer {

    private static final Logger log = LoggerFactory.getLogger(MetricConsumer.class);

    private final AggregationService aggregationService;
    private final AnomalyDetectionService anomalyService;
    private final ObjectMapper objectMapper;

    public MetricConsumer(AggregationService aggregationService,
                          AnomalyDetectionService anomalyService,
                          ObjectMapper objectMapper) {
        this.aggregationService = aggregationService;
        this.anomalyService = anomalyService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaConfig.EVENTS_TOPIC, groupId = "pulse-metrics-group")
    public void consumeEvent(String message) {
        try {
            MetricEvent event = objectMapper.readValue(message, MetricEvent.class);
            log.debug("Consumed raw metric event: {}", event);

            // 1. Record the event for windowed aggregations
            aggregationService.recordEvent(event);

            // 2. Perform Z-score anomaly checking
            anomalyService.checkAnomaly(event);

        } catch (Exception e) {
            log.error("Error processing consumed Kafka event: {}", message, e);
        }
    }
}
