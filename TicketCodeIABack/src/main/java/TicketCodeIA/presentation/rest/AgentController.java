package TicketCodeIA.presentation.rest;

import TicketCodeIA.infrastructure.agent.ExpertAgentAdapter;
import TicketCodeIA.presentation.dto.request.ExpertChatRequest;
import TicketCodeIA.presentation.dto.response.ExpertChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {

    private final ExpertAgentAdapter expertAgentAdapter;

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
