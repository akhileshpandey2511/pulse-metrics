package com.pulse.metrics.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "support_tickets")
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "user_query", nullable = false)
    private String userQuery;

    @Column(name = "matched_intent")
    private String matchedIntent;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "status", nullable = false)
    private String status; // 'AUTO_RESOLVED', 'PENDING_ESCALATION', 'ESCALATED'

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public SupportTicket() {}

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

    public String getUserQuery() {
        return userQuery;
    }

    public void setUserQuery(String userQuery) {
        this.userQuery = userQuery;
    }

    public String getMatchedIntent() {
        return matchedIntent;
    }

    public void setMatchedIntent(String matchedIntent) {
        this.matchedIntent = matchedIntent;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
