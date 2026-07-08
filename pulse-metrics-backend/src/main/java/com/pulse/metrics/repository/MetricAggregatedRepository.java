package com.pulse.metrics.repository;

import com.pulse.metrics.model.MetricAggregated;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MetricAggregatedRepository extends JpaRepository<MetricAggregated, Long> {

    List<MetricAggregated> findByTenantIdAndWindowTypeAndEventTypeAndWindowStartAfterOrderByWindowStartAsc(
            String tenantId, String windowType, String eventType, LocalDateTime start);

    @Query("SELECT DISTINCT m.eventType FROM MetricAggregated m WHERE m.tenantId = :tenantId")
    List<String> findDistinctEventTypesByTenantId(String tenantId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM MetricAggregated m WHERE m.windowStart < :cutoff")
    void deleteOlderThan(LocalDateTime cutoff);
}
