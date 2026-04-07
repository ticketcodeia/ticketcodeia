package TicketCodeIA.controller;

import TicketCodeIA.dto.TicketRequest;
import TicketCodeIA.dto.TicketResponse;
import TicketCodeIA.dto.TicketStats;
import TicketCodeIA.enums.TicketStatus;
import TicketCodeIA.service.AgentLogService;
import TicketCodeIA.service.TicketService;
import TicketCodeIA.agent.AgentOrchestrator;
import TicketCodeIA.entity.AgentLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final AgentOrchestrator agentOrchestrator;
    private final AgentLogService agentLogService;

    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(@RequestBody TicketRequest request) {
        TicketResponse ticket = ticketService.createTicket(request);
        return ResponseEntity.ok(ticket);
    }

    @GetMapping
    public ResponseEntity<List<TicketResponse>> getAllTickets(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) TicketStatus status) {
        List<TicketResponse> tickets = ticketService.getAllTickets(projectId, status);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicketById(@PathVariable Long id) {
        TicketResponse ticket = ticketService.getTicketById(id);
        return ResponseEntity.ok(ticket);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TicketResponse> updateTicket(
            @PathVariable Long id,
            @RequestBody TicketRequest request) {
        TicketResponse ticket = ticketService.updateTicket(id, request);
        return ResponseEntity.ok(ticket);
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<TicketResponse> processTicket(
            @PathVariable Long id,
            @RequestBody(required = false) TicketCodeIA.dto.ProcessTicketRequest request) {
        if (request == null) {
            request = new TicketCodeIA.dto.ProcessTicketRequest(true, true);
        }
        ticketService.saveAgentFlags(id, request.isEnableCodeReview(), request.isEnableTesting());
        agentOrchestrator.processTicketAsync(id, request.isEnableCodeReview(), request.isEnableTesting());
        TicketResponse ticket = ticketService.getTicketById(id);
        return ResponseEntity.accepted().body(ticket);
    }

    @GetMapping("/stats")
    public ResponseEntity<TicketStats> getStats() {
        TicketStats stats = ticketService.getStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/{id}/logs")
    public ResponseEntity<List<AgentLog>> getTicketLogs(@PathVariable Long id) {
        List<AgentLog> logs = agentLogService.getLogsByTicketId(id);
        return ResponseEntity.ok(logs);
    }
}
