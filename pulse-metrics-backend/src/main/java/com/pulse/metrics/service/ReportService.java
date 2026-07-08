package com.pulse.metrics.service;

import com.pulse.metrics.model.MetricAggregated;
import com.pulse.metrics.repository.MetricAggregatedRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);
    private final MetricAggregatedRepository metricRepository;

    public ReportService(MetricAggregatedRepository metricRepository) {
        this.metricRepository = metricRepository;
    }

    public String generateCsvReport(String tenantId, String eventType, String windowType) {
        log.info("Generating report for tenant: {}, eventType: {}, windowType: {}", tenantId, eventType, windowType);

        LocalDateTime start = LocalDateTime.now().minusDays(7);
        List<MetricAggregated> metrics = metricRepository
                .findByTenantIdAndWindowTypeAndEventTypeAndWindowStartAfterOrderByWindowStartAsc(
                        tenantId, windowType, eventType, start);

        File reportsDir = new File("./reports");
        if (!reportsDir.exists()) {
            reportsDir.mkdirs();
        }

        String filename = String.format("report_%s_%s_%s_%s.csv",
                tenantId, eventType, windowType, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
        File reportFile = new File(reportsDir, filename);

        try (FileWriter writer = new FileWriter(reportFile)) {
            writer.write("ID,TenantId,WindowType,EventType,ValueSum,ValueCount,ValueAvg,WindowStart,WindowEnd\n");
            for (MetricAggregated m : metrics) {
                writer.write(String.format("%d,%s,%s,%s,%.2f,%d,%.2f,%s,%s\n",
                        m.getId(), m.getTenantId(), m.getWindowType(), m.getEventType(),
                        m.getValueSum(), m.getValueCount(), m.getValueAvg(),
                        m.getWindowStart().toString(), m.getWindowEnd().toString()));
            }
            log.info("CSV Report written to: {}", reportFile.getAbsolutePath());
            return reportFile.getName();
        } catch (IOException e) {
            log.error("Failed to write CSV report", e);
            throw new RuntimeException("Report generation failed", e);
        }
    }
}
