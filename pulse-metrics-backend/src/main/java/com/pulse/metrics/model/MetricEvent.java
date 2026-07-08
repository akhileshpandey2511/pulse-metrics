package com.pulse.metrics.model;

import java.io.Serializable;

public class MetricEvent implements Serializable {
    private String tenantId;
    private String eventType;
    private Double value;
    private Long timestamp;

    public MetricEvent() {
    }

    public MetricEvent(String tenantId, String eventType, Double value, Long timestamp) {
        this.tenantId = tenantId;
        this.eventType = eventType;
        this.value = value;
        this.timestamp = timestamp;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "MetricEvent{" +
                "tenantId='" + tenantId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", value=" + value +
                ", timestamp=" + timestamp +
                '}';
    }
}
