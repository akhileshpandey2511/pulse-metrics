package com.pulse.metrics.service;

import com.pulse.metrics.model.Alert;
import com.pulse.metrics.model.MetricAggregated;
import com.pulse.metrics.repository.AlertRepository;
import com.pulse.metrics.repository.MetricAggregatedRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EmailReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(EmailReportScheduler.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final MetricAggregatedRepository metricRepository;
    private final AlertRepository alertRepository;
    private final PdfReportService pdfReportService;
    private final EmailService emailService;

    public EmailReportScheduler(RedisTemplate<String, Object> redisTemplate,
                                MetricAggregatedRepository metricRepository,
                                AlertRepository alertRepository,
                                PdfReportService pdfReportService,
                                EmailService emailService) {
        this.redisTemplate = redisTemplate;
        this.metricRepository = metricRepository;
        this.alertRepository = alertRepository;
        this.pdfReportService = pdfReportService;
        this.emailService = emailService;
    }

    // Runs every day at 8:00 AM to send the 24-hour daily dashboard summary PDF report via email
    @Scheduled(cron = "0 0 8 * * *")
    public void generateAndSendScheduledReports() {
        log.info("Starting daily 8:00 AM PDF Dashboard Summary email generation...");

        // Retrieve active tenants from Redis, default to predefined demo tenants if empty
        Set<Object> tenantsObj = redisTemplate.opsForSet().members("pulse:tenants");
        List<String> tenants = new ArrayList<>();
        if (tenantsObj != null && !tenantsObj.isEmpty()) {
            tenants = tenantsObj.stream().map(String::valueOf).collect(Collectors.toList());
        } else {
            tenants = Arrays.asList("tenant_1", "tenant_2", "tenant_3");
        }

        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);

        for (String tenantId : tenants) {
            try {
                // 1. Gather all metric aggregates generated in the last 24 hours for this tenant (using hourly resolution)
                List<MetricAggregated> tenantMetrics = new ArrayList<>();
                Set<Object> typesObj = redisTemplate.opsForSet().members("pulse:tenant:" + tenantId + ":types");
                List<String> eventTypes = new ArrayList<>();
                if (typesObj != null && !typesObj.isEmpty()) {
                    eventTypes = typesObj.stream().map(String::valueOf).collect(Collectors.toList());
                } else {
                    eventTypes = Arrays.asList("cpu_utilization", "request_count", "error_rate");
                }

                for (String eventType : eventTypes) {
                    List<MetricAggregated> list = metricRepository
                            .findByTenantIdAndWindowTypeAndEventTypeAndWindowStartAfterOrderByWindowStartAsc(
                                    tenantId, "1h", eventType, cutoffTime);
                    tenantMetrics.addAll(list);
                }

                // 2. Gather all alerts triggered in the last 24 hours for this tenant
                List<Alert> allAlerts = alertRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
                List<Alert> recentAlerts = allAlerts.stream()
                        .filter(a -> a.getCreatedAt() != null && a.getCreatedAt().isAfter(cutoffTime))
                        .collect(Collectors.toList());

                // 3. Compile statistics into a PDF report
                File reportFile = pdfReportService.generateDashboardPdf(tenantId, tenantMetrics, recentAlerts);

                // 4. Send the compiled report as an email attachment
                String administratorEmail = "akhileshpandey2510@gmail.com";
                String subject = String.format("PulseMetrics Daily Dashboard Summary (24 Hours): %s", tenantId.toUpperCase());
                String body = String.format(
                        "Hello Administrator,\n\n" +
                        "Attached is the automated daily dashboard summary report (last 24 hours) for your tenant: %s.\n\n" +
                        "Summary Details:\n" +
                        "- Aggregated hourly data points compiled: %d\n" +
                        "- Anomaly Alerts triggered in the last 24 hours: %d\n\n" +
                        "Best regards,\n" +
                        "PulseMetrics Platform Engine",
                        tenantId.toUpperCase(),
                        tenantMetrics.size(),
                        recentAlerts.size()
                );

                emailService.sendEmailWithAttachment(administratorEmail, subject, body, reportFile);

            } catch (Exception e) {
                log.error("Failed to generate and email scheduled report for tenant {}", tenantId, e);
            }
        }

        log.info("Daily 8:00 AM PDF Dashboard Summary emailing task completed.");
    }
}
