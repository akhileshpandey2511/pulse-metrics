package com.pulse.metrics.repository;

import com.pulse.metrics.model.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByTenantIdOrderByCreatedAtDesc(String tenantId);
}
