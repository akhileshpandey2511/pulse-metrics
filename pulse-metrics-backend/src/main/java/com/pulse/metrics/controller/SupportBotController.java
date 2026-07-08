package com.pulse.metrics.controller;

import com.pulse.metrics.config.TenantContext;
import com.pulse.metrics.model.SupportTicket;
import com.pulse.metrics.repository.SupportTicketRepository;
import com.pulse.metrics.service.SupportBotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/support")
public class SupportBotController {

    private final SupportBotService supportBotService;
    private final SupportTicketRepository ticketRepository;

    public SupportBotController(SupportBotService supportBotService, SupportTicketRepository ticketRepository) {
        this.supportBotService = supportBotService;
        this.ticketRepository = ticketRepository;
    }

    @PostMapping("/query")
    public ResponseEntity<Map<String, Object>> submitQuery(@RequestBody Map<String, String> requestBody) {
        String query = requestBody.get("query");
        if (query == null || query.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Query must not be empty"));
        }
        String tenantId = TenantContext.getCurrentTenant();
        Map<String, Object> response = supportBotService.handleQuery(tenantId, query);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tickets")
    public ResponseEntity<List<SupportTicket>> getTickets() {
        String tenantId = TenantContext.getCurrentTenant();
        List<SupportTicket> tickets = ticketRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        return ResponseEntity.ok(tickets);
    }
}
