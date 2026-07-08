package com.pulse.metrics.service;

import com.pulse.metrics.model.SupportTicket;
import com.pulse.metrics.repository.SupportTicketRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SupportBotService {

    private static final Logger log = LoggerFactory.getLogger(SupportBotService.class);
    private final RedisTemplate<String, Object> redisTemplate;
    private final SupportTicketRepository ticketRepository;

    private static final String FAQ_REDIS_PREFIX = "pulse:faq:";

    public SupportBotService(RedisTemplate<String, Object> redisTemplate, SupportTicketRepository ticketRepository) {
        this.redisTemplate = redisTemplate;
        this.ticketRepository = ticketRepository;
    }

    @PostConstruct
    public void seedFAQs() {
        // Seed default FAQs into Redis
        Map<String, String> faqs = new HashMap<>();
        faqs.put("api_slow", "Ensure Redis cache is enabled. Check database connection pool sizes in application.yml. Index target query columns.");
        faqs.put("kafka_lag", "Increase partition count and deploy more consumers in the group. Check for long-running blocking operations in listeners.");
        faqs.put("anomaly_threshold", "Anomalies are triggered when the Z-score of event throughput exceeds 2.5 (deviating from a 30-event rolling mean).");
        faqs.put("multi_tenancy", "Tenant isolation is enforced via custom claims in JWT tokens. Database filters filter by tenant_id on all queries.");
        faqs.put("db_partition", "Aggregate metrics tables are partitioned by window_start ranges to optimize historical querying and cleaning.");

        for (Map.Entry<String, String> entry : faqs.entrySet()) {
            redisTemplate.opsForValue().set(FAQ_REDIS_PREFIX + entry.getKey(), entry.getValue());
        }
        log.info("Successfully seeded FAQ cache in Redis");
    }

    public Map<String, Object> handleQuery(String tenantId, String userQuery) {
        String cleanedQuery = userQuery.toLowerCase().trim();
        String matchedIntent = "unknown";
        double maxConfidence = 0.0;

        // Simple keyword-matching heuristics
        Map<String, List<String>> intentKeywords = new HashMap<>();
        intentKeywords.put("api_slow", Arrays.asList("slow", "latency", "response", "speed", "api", "redis", "cache"));
        intentKeywords.put("kafka_lag", Arrays.asList("kafka", "lag", "consumer", "queue", "pipeline", "message"));
        intentKeywords.put("anomaly_threshold", Arrays.asList("anomaly", "z-score", "threshold", "alert", "trigger"));
        intentKeywords.put("multi_tenancy", Arrays.asList("tenant", "isolation", "security", "jwt", "tenant_id", "access"));
        intentKeywords.put("db_partition", Arrays.asList("partition", "postgresql", "postgres", "table", "cockroach", "db"));

        for (Map.Entry<String, List<String>> entry : intentKeywords.entrySet()) {
            double matches = 0;
            for (String kw : entry.getValue()) {
                if (cleanedQuery.contains(kw)) {
                    matches++;
                }
            }
            double confidence = matches / entry.getValue().size();
            if (confidence > maxConfidence) {
                maxConfidence = confidence;
                matchedIntent = entry.getKey();
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("query", userQuery);

        if (maxConfidence >= 0.25) {
            String answer = (String) redisTemplate.opsForValue().get(FAQ_REDIS_PREFIX + matchedIntent);
            response.put("response", answer);
            response.put("status", "RESOLVED");
            response.put("confidence", maxConfidence);
            response.put("intent", matchedIntent);

            // Log resolved query
            saveTicket(tenantId, userQuery, matchedIntent, maxConfidence, "AUTO_RESOLVED");
        } else {
            response.put("response", "I'm sorry, I couldn't match your query to any known issues. I have escalated this and created a support ticket.");
            response.put("status", "ESCALATED");
            response.put("confidence", maxConfidence);
            response.put("intent", "unknown");

            // Log escalated ticket
            SupportTicket ticket = saveTicket(tenantId, userQuery, "unknown", maxConfidence, "ESCALATED");
            response.put("ticketId", ticket.getId());
        }

        return response;
    }

    private SupportTicket saveTicket(String tenantId, String query, String intent, double confidence, String status) {
        SupportTicket ticket = new SupportTicket();
        ticket.setTenantId(tenantId);
        ticket.setUserQuery(query);
        ticket.setMatchedIntent(intent);
        ticket.setConfidence(confidence);
        ticket.setStatus(status);
        return ticketRepository.save(ticket);
    }
}
