package TicketCodeIA.presentation.rest;

import TicketCodeIA.application.usecase.agent.GenerateTicketsFromRequirementsUseCase;
import TicketCodeIA.presentation.dto.request.RequirementsRequest;
import TicketCodeIA.presentation.dto.response.TicketResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {

    private final GenerateTicketsFromRequirementsUseCase generateTicketsUseCase;

    @PostMapping("/generate-tickets")
    public ResponseEntity<List<TicketResponse>> generateTickets(@RequestBody RequirementsRequest request) {
        List<TicketResponse> tickets = generateTicketsUseCase
                .execute(request.getRequirements(), request.getProjectId())
                .stream().map(TicketResponse::fromResult).toList();
        return ResponseEntity.ok(tickets);
    }
}
