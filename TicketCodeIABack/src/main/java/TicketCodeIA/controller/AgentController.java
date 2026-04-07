package TicketCodeIA.controller;

import TicketCodeIA.dto.RequirementsRequest;
import TicketCodeIA.dto.TicketResponse;
import TicketCodeIA.agent.POAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {

    private final POAgent poAgent;

    @PostMapping("/generate-tickets")
    public ResponseEntity<List<TicketResponse>> generateTickets(@RequestBody RequirementsRequest request) {
        List<TicketResponse> tickets = poAgent.generateTicketsFromRequirements(
                request.getRequirements(), request.getProjectId());
        return ResponseEntity.ok(tickets);
    }
}
