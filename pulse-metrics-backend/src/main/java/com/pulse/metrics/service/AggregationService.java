package com.pulse.metrics.service;

import com.pulse.metrics.model.MetricAggregated;
import com.pulse.metrics.model.MetricEvent;
import com.pulse.metrics.repository.MetricAggregatedRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Service
public class AggregationService {

    private static final Logger log = LoggerFactory.getLogger(AggregationService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final MetricAggregatedRepository metricRepository;

    public AggregationService(RedisTemplate<String, Object> redisTemplate, MetricAggregatedRepository metricRepository) {
        this.redisTemplate = redisTemplate;
        this.metricRepository = metricRepository;
    }

    public void recordEvent(MetricEvent event) {
        String tenantId = event.getTenantId();
        String eventType = event.getEventType();
        Double value = event.getValue();

        // Increment sum and count in Redis
        String sumKey = "pulse:raw:" + tenantId + ":" + eventType + ":sum";
        String countKey = "pulse:raw:" + tenantId + ":" + eventType + ":count";

        redisTemplate.opsForValue().increment(sumKey, value);
        redisTemplate.opsForValue().increment(countKey, 1);

        // Keep track of active tenants and event types
        redisTemplate.opsForSet().add("pulse:tenants", tenantId);
        redisTemplate.opsForSet().add("pulse:tenant:" + tenantId + ":types", eventType);
    }

    // Runs every 10 seconds to flush raw redis aggregations to CockroachDB (aggregated as 1m window for demo)
    @Scheduled(fixedRate = 10000)
    @Transactional
    public void flushOneMinuteAggregates() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.minusSeconds(10);
        LocalDateTime windowEnd = now;

        Set<Object> tenantsObj = redisTemplate.opsForSet().members("pulse:tenants");
        if (tenantsObj == null) return;

        for (Object t : tenantsObj) {
            String tenantId = (String) t;
            Set<Object> typesObj = redisTemplate.opsForSet().members("pulse:tenant:" + tenantId + ":types");
            if (typesObj == null) continue;

            for (Object ty : typesObj) {
                String eventType = (String) ty;

                String sumKey = "pulse:raw:" + tenantId + ":" + eventType + ":sum";
                String countKey = "pulse:raw:" + tenantId + ":" + eventType + ":count";

                String sumValStr = String.valueOf(redisTemplate.opsForValue().get(sumKey));
                String countValStr = String.valueOf(redisTemplate.opsForValue().get(countKey));

                if (sumValStr == null || "null".equals(sumValStr) || countValStr == null || "null".equals(countValStr)) {
                    continue;
                }

                double sum = Double.parseDouble(sumValStr);
                long count = Long.parseLong(countValStr);

                if (count > 0) {
                    MetricAggregated ma = new MetricAggregated();
                    ma.setTenantId(tenantId);
                    ma.setEventType(eventType);
                    ma.setWindowType("1m");
                    ma.setValueSum(sum);
                    ma.setValueCount(count);
                    ma.setValueAvg(sum / count);
                    ma.setWindowStart(windowStart);
                    ma.setWindowEnd(windowEnd);

                    metricRepository.save(ma);

                    // Reset keys
                    redisTemplate.delete(sumKey);
                    redisTemplate.delete(countKey);
                }
            }
        }
        log.info("Flushed 1-minute aggregates to CockroachDB for window: {} to {}", windowStart, windowEnd);
    }

    // Aggregate 5m windows from 1m data (runs every 30 seconds for demo)
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void aggregateFiveMinuteWindows() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.minusMinutes(5);
        LocalDateTime windowEnd = now;
        performDatabaseRollup("1m", "5m", windowStart, windowEnd);
    }

    // Aggregate 1h windows from 5m data (runs every 60 seconds for demo)
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void aggregateOneHourWindows() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.minusHours(1);
        LocalDateTime windowEnd = now;
        performDatabaseRollup("1m", "1h", windowStart, windowEnd); // Roll up directly from 1m for demo completeness
    }

    // Aggregate 24h windows from 1h data (runs every 120 seconds for demo)
    @Scheduled(fixedRate = 120000)
    @Transactional
    public void aggregateTwentyFourHourWindows() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.minusDays(1);
        LocalDateTime windowEnd = now;
        performDatabaseRollup("1m", "24h", windowStart, windowEnd); // Roll up directly from 1m for demo completeness
    }

    private void performDatabaseRollup(String srcWindow, String destWindow, LocalDateTime start, LocalDateTime end) {
        Set<Object> tenantsObj = redisTemplate.opsForSet().members("pulse:tenants");
        if (tenantsObj == null) return;

        for (Object t : tenantsObj) {
            String tenantId = (String) t;
            Set<Object> typesObj = redisTemplate.opsForSet().members("pulse:tenant:" + tenantId + ":types");
            if (typesObj == null) continue;

            for (Object ty : typesObj) {
                String eventType = (String) ty;

                List<MetricAggregated> sources = metricRepository
                        .findByTenantIdAndWindowTypeAndEventTypeAndWindowStartAfterOrderByWindowStartAsc(
                                tenantId, srcWindow, eventType, start.minusSeconds(1));

                if (sources.isEmpty()) continue;

                double sum = 0;
                long count = 0;

                for (MetricAggregated src : sources) {
                    if (src.getWindowEnd().isBefore(end.plusSeconds(1))) {
                        sum += src.getValueSum();
                        count += src.getValueCount();
                    }
                }

                if (count > 0) {
                    MetricAggregated ma = new MetricAggregated();
                    ma.setTenantId(tenantId);
                    ma.setEventType(eventType);
                    ma.setWindowType(destWindow);
                    ma.setValueSum(sum);
                    ma.setValueCount(count);
                    ma.setValueAvg(sum / count);
                    ma.setWindowStart(start);
                    ma.setWindowEnd(end);

                    metricRepository.save(ma);
                }
            }
        }
        log.info("Completed database rollup from {} to {} for window: {} to {}", srcWindow, destWindow, start, end);
    }

    // Clean up metrics data older than 24 hours (runs every 60 seconds)
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void purgeOldMetrics() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        metricRepository.deleteOlderThan(cutoff);
        log.info("Purged historical aggregates older than 24 hours (cutoff: {})", cutoff);
    }
}
