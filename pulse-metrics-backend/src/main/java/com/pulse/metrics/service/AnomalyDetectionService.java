package com.pulse.metrics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulse.metrics.config.KafkaConfig;
import com.pulse.metrics.model.Alert;
import com.pulse.metrics.model.MetricEvent;
import com.pulse.metrics.repository.AlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AnomalyDetectionService {

    private static final Logger log = LoggerFactory.getLogger(AnomalyDetectionService.class);
    private static final int HISTORY_SIZE = 30;
    private static final double Z_SCORE_THRESHOLD = 2.5;

    private final RedisTemplate<String, Object> redisTemplate;
    private final AlertRepository alertRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public AnomalyDetectionService(RedisTemplate<String, Object> redisTemplate,
                                   AlertRepository alertRepository,
                                   KafkaTemplate<String, String> kafkaTemplate,
                                   ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.alertRepository = alertRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void checkAnomaly(MetricEvent event) {
        String tenantId = event.getTenantId();
        String eventType = event.getEventType();
        Double value = event.getValue();

        String historyKey = "pulse:history:" + tenantId + ":" + eventType;

        // Push new value to Redis list
        redisTemplate.opsForList().rightPush(historyKey, value);
        redisTemplate.opsForList().trim(historyKey, -HISTORY_SIZE, -1);

        // Get history
        List<Object> historyObj = redisTemplate.opsForList().range(historyKey, 0, -1);
        if (historyObj == null || historyObj.size() < 10) {
            // Need at least 10 data points to establish a baseline
            return;
        }

        List<Double> history = historyObj.stream()
                .map(o -> Double.parseDouble(String.valueOf(o)))
                .collect(Collectors.toList());

        // Calculate mean
        double mean = history.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        // Calculate standard deviation
        double variance = 0.0;
        for (double val : history) {
            variance += Math.pow(val - mean, 2);
        }
        double stdDev = Math.sqrt(variance / history.size());

        if (stdDev == 0.0) {
            return; // Avoid division by zero
        }

        // Calculate Z-Score
        double zScore = Math.abs(value - mean) / stdDev;

        log.debug("Checking Anomaly for {}-{}: value={}, mean={}, stdDev={}, Z-Score={}", 
                tenantId, eventType, value, mean, stdDev, zScore);

        if (zScore > Z_SCORE_THRESHOLD) {
            triggerAlert(event, value, mean, zScore);
        }
    }

    private void triggerAlert(MetricEvent event, double value, double threshold, double zScore) {
        String message = String.format("Anomaly detected in %s: value %.2f (average: %.2f) with Z-score %.2f",
                event.getEventType(), value, threshold, zScore);

        Alert alert = new Alert();
        alert.setTenantId(event.getTenantId());
        alert.setMetricName(event.getEventType());
        alert.setAlertValue(value);
        alert.setThreshold(threshold);
        alert.setzScore(zScore);
        alert.setStatus("ACTIVE");
        alert.setMessage(message);

        Alert savedAlert = alertRepository.save(alert);
        log.warn("ALERT FIRED: {}", message);

        // Publish Alert to Kafka anomalies topic for consumption
        try {
            String jsonAlert = objectMapper.writeValueAsString(savedAlert);
            kafkaTemplate.send(KafkaConfig.ANOMALIES_TOPIC, event.getTenantId(), jsonAlert);
        } catch (Exception e) {
            log.error("Failed to serialize and send alert to Kafka", e);
        }
    }
}
