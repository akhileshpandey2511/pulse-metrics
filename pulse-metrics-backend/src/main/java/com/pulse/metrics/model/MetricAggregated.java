package com.pulse.metrics.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "metrics_aggregated")
public class MetricAggregated {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "window_type", nullable = false)
    private String windowType;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "value_sum", nullable = false)
    private Double valueSum;

    @Column(name = "value_count", nullable = false)
    private Long valueCount;

    @Column(name = "value_avg", nullable = false)
    private Double valueAvg;

    @Column(name = "window_start", nullable = false)
    private LocalDateTime windowStart;

    @Column(name = "window_end", nullable = false)
    private LocalDateTime windowEnd;

    public MetricAggregated() {}

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

    public String getWindowType() {
        return windowType;
    }

    public void setWindowType(String windowType) {
        this.windowType = windowType;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Double getValueSum() {
        return valueSum;
    }

    public void setValueSum(Double valueSum) {
        this.valueSum = valueSum;
    }

    public Long getValueCount() {
        return valueCount;
    }

    public void setValueCount(Long valueCount) {
        this.valueCount = valueCount;
    }

    public Double getValueAvg() {
        return valueAvg;
    }

    public void setValueAvg(Double valueAvg) {
        this.valueAvg = valueAvg;
    }

    public LocalDateTime getWindowStart() {
        return windowStart;
    }

    public void setWindowStart(LocalDateTime windowStart) {
        this.windowStart = windowStart;
    }

    public LocalDateTime getWindowEnd() {
        return windowEnd;
    }

    public void setWindowEnd(LocalDateTime windowEnd) {
        this.windowEnd = windowEnd;
    }
}
