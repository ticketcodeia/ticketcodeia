package TicketCodeIA.presentation.rest;

import TicketCodeIA.application.usecase.agentlog.GetAgentLogsUseCase;
import TicketCodeIA.infrastructure.sse.SseService;
import TicketCodeIA.presentation.dto.response.AgentLogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class SseController {

    private final SseService sseService;
    private final GetAgentLogsUseCase getAgentLogsUseCase;

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        return sseService.subscribe();
    }

    @GetMapping("/recent-activity")
    public List<AgentLogResponse> getRecentActivity() {
        return getAgentLogsUseCase.getRecentLogs().stream()
                .map(AgentLogResponse::fromResult).toList();
    }
}
