package TicketCodeIA.presentation.rest;

import TicketCodeIA.application.usecase.agent.GenerateTicketsFromRequirementsUseCase;
import TicketCodeIA.infrastructure.agent.ExpertAgentAdapter;
import TicketCodeIA.presentation.dto.request.ExpertChatRequest;
import TicketCodeIA.presentation.dto.request.RequirementsRequest;
import TicketCodeIA.presentation.dto.response.ExpertChatResponse;
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
    private final ExpertAgentAdapter expertAgentAdapter;

    @PostMapping("/generate-tickets")
    public ResponseEntity<List<TicketResponse>> generateTickets(@RequestBody RequirementsRequest request) {
        List<TicketResponse> tickets = generateTicketsUseCase
                .execute(request.getRequirements(), request.getProjectId())
                .stream().map(TicketResponse::fromResult).toList();
        return ResponseEntity.ok(tickets);
    }

    @PostMapping("/expert/chat")
    public ResponseEntity<ExpertChatResponse> expertChat(@RequestBody ExpertChatRequest request) {
        String response = expertAgentAdapter.chat(
                request.getSessionId(), request.getMessage(), request.getProjectId());
        return ResponseEntity.ok(new ExpertChatResponse(request.getSessionId(), response));
    }

    @DeleteMapping("/expert/session/{sessionId}")
    public ResponseEntity<Void> clearExpertSession(@PathVariable String sessionId) {
        expertAgentAdapter.clearSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}
