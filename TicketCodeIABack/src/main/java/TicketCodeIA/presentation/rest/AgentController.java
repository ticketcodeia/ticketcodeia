package TicketCodeIA.presentation.rest;

import TicketCodeIA.application.query.ChatMessageResult;
import TicketCodeIA.application.usecase.chat.ClearChatSessionUseCase;
import TicketCodeIA.application.usecase.chat.GetChatHistoryUseCase;
import TicketCodeIA.application.usecase.chat.SendChatMessageUseCase;
import TicketCodeIA.presentation.dto.request.ExpertChatRequest;
import TicketCodeIA.presentation.dto.response.ExpertChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {

    private final SendChatMessageUseCase sendChatMessageUseCase;
    private final GetChatHistoryUseCase getChatHistoryUseCase;
    private final ClearChatSessionUseCase clearChatSessionUseCase;

    @PostMapping("/expert/chat")
    public ResponseEntity<ExpertChatResponse> expertChat(@RequestBody ExpertChatRequest request) {
        ChatMessageResult result = sendChatMessageUseCase.execute(
                request.getSessionId(), request.getMessage(), request.getProjectId());
        return ResponseEntity.ok(new ExpertChatResponse(request.getSessionId(), result.content()));
    }

    @GetMapping("/expert/session/{sessionId}/history")
    public ResponseEntity<List<ChatMessageResult>> getChatHistory(@PathVariable String sessionId) {
        return ResponseEntity.ok(getChatHistoryUseCase.bySession(sessionId));
    }

    @GetMapping("/expert/project/{projectId}/history")
    public ResponseEntity<List<ChatMessageResult>> getProjectChatHistory(@PathVariable Long projectId) {
        return ResponseEntity.ok(getChatHistoryUseCase.byProject(projectId));
    }

    @DeleteMapping("/expert/session/{sessionId}")
    public ResponseEntity<Void> clearExpertSession(@PathVariable String sessionId) {
        clearChatSessionUseCase.execute(sessionId);
        return ResponseEntity.noContent().build();
    }
}
