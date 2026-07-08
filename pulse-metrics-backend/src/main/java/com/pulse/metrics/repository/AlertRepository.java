package com.pulse.metrics.repository;

import com.pulse.metrics.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    List<Alert> findByTenantIdAndStatusOrderByCreatedAtDesc(String tenantId, String status);
}
