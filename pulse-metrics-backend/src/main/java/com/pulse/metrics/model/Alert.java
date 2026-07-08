package com.pulse.metrics.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "metric_name", nullable = false)
    private String metricName;

    @Column(name = "alert_value", nullable = false)
    private Double alertValue;

    @Column(name = "threshold", nullable = false)
    private Double threshold;

    @Column(name = "z_score", nullable = false)
    private Double zScore;

    @Column(name = "status", nullable = false)
    private String status; // 'ACTIVE', 'ACKNOWLEDGED', 'RESOLVED'

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Alert() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public Double getAlertValue() {
        return alertValue;
    }

    public void setAlertValue(Double alertValue) {
        this.alertValue = alertValue;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    public Double getzScore() {
        return zScore;
    }

    public void setzScore(Double zScore) {
        this.zScore = zScore;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
