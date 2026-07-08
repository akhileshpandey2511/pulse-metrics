package com.pulse.metrics.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.pulse.metrics.model.Alert;
import com.pulse.metrics.model.MetricAggregated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfReportService {

    private static final Logger log = LoggerFactory.getLogger(PdfReportService.class);

    public File generateDashboardPdf(String tenantId, List<MetricAggregated> metrics, List<Alert> alerts) {
        File reportsDir = new File("./reports/scheduled");
        if (!reportsDir.exists()) {
            reportsDir.mkdirs();
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = String.format("summary_report_%s_%s.pdf", tenantId, timestamp);
        File pdfFile = new File(reportsDir, filename);

        Document document = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(document, new FileOutputStream(pdfFile));
            document.open();

            // Font configurations
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, new Color(16, 185, 129)); // Emerald Theme
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.DARK_GRAY);
            Font subHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.GRAY);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);

            // Document Header
            Paragraph title = new Paragraph("PulseMetrics - Real-Time Dashboard Summary", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            Paragraph metadata = new Paragraph(String.format(
                    "Tenant ID: %s\nGenerated At: %s\nReporting Period: Last 5 Minutes\nInfrastructure: Kafka + Redis + CockroachDB",
                    tenantId.toUpperCase(),
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            ), subHeaderFont);
            metadata.setSpacingAfter(20);
            document.add(metadata);

            // Add thin divider line
            Paragraph divider = new Paragraph("----------------------------------------------------------------------------------------------------------------", subHeaderFont);
            divider.setSpacingAfter(15);
            document.add(divider);

            // Section 1: Aggregated Metrics
            Paragraph sec1Header = new Paragraph("Aggregated Metric Statistics (Last 5m)", headerFont);
            sec1Header.setSpacingAfter(10);
            document.add(sec1Header);

            if (metrics.isEmpty()) {
                Paragraph emptyMetrics = new Paragraph("No aggregated metrics found for this period. Ensure the simulator is running.", bodyFont);
                emptyMetrics.setSpacingAfter(20);
                document.add(emptyMetrics);
            } else {
                PdfPTable table = new PdfPTable(5); // 5 columns
                table.setWidthPercentage(100);
                table.setSpacingAfter(20);

                // Table Headers
                String[] headers = {"Metric Type", "Average Value", "Sum Value", "Event Count", "Window End"};
                for (String header : headers) {
                    PdfPCell cell = new PdfPCell(new Phrase(header, tableHeaderFont));
                    cell.setBackgroundColor(new Color(16, 185, 129)); // Emerald Header
                    cell.setPadding(6);
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    table.addCell(cell);
                }

                // Table Data
                for (MetricAggregated m : metrics) {
                    table.addCell(new PdfPCell(new Phrase(m.getEventType().replace("_", " ").toUpperCase(), bodyFont)));
                    table.addCell(new PdfPCell(new Phrase(String.format("%.2f", m.getValueAvg()), bodyFont)));
                    table.addCell(new PdfPCell(new Phrase(String.format("%.2f", m.getValueSum()), bodyFont)));
                    table.addCell(new PdfPCell(new Phrase(String.valueOf(m.getValueCount()), bodyFont)));
                    table.addCell(new PdfPCell(new Phrase(m.getWindowEnd().format(DateTimeFormatter.ofPattern("HH:mm:ss")), bodyFont)));
                }
                document.add(table);
            }

            // Section 2: Triggered Anomaly Alerts
            Paragraph sec2Header = new Paragraph("Fired Anomaly Alerts (Last 5m)", headerFont);
            sec2Header.setSpacingAfter(10);
            document.add(sec2Header);

            if (alerts.isEmpty()) {
                Paragraph emptyAlerts = new Paragraph("No anomaly alerts detected in this period. Pipeline running within normal thresholds.", bodyFont);
                emptyAlerts.setSpacingAfter(20);
                document.add(emptyAlerts);
            } else {
                PdfPTable alertTable = new PdfPTable(4); // 4 columns
                alertTable.setWidthPercentage(100);
                alertTable.setSpacingAfter(20);

                // Table Headers
                String[] alertHeaders = {"Metric Name", "Value", "Z-Score", "Alert Message"};
                for (String header : alertHeaders) {
                    PdfPCell cell = new PdfPCell(new Phrase(header, tableHeaderFont));
                    cell.setBackgroundColor(new Color(239, 68, 68)); // Red Header for Alerts
                    cell.setPadding(6);
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    alertTable.addCell(cell);
                }

                // Table Data
                for (Alert a : alerts) {
                    alertTable.addCell(new PdfPCell(new Phrase(a.getMetricName().toUpperCase(), bodyFont)));
                    alertTable.addCell(new PdfPCell(new Phrase(String.format("%.2f", a.getAlertValue()), bodyFont)));
                    alertTable.addCell(new PdfPCell(new Phrase(String.format("%.2f", a.getzScore()), bodyFont)));
                    alertTable.addCell(new PdfPCell(new Phrase(a.getMessage(), bodyFont)));
                }
                document.add(alertTable);
            }

            // Document Footer
            Paragraph footer = new Paragraph("Automated report generated by PulseMetrics Platform Engine.", subHeaderFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(30);
            document.add(footer);

            document.close();
            log.info("PDF Report generated successfully: {}", pdfFile.getAbsolutePath());
            return pdfFile;

        } catch (Exception e) {
            log.error("Failed to generate PDF Report", e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }
}
