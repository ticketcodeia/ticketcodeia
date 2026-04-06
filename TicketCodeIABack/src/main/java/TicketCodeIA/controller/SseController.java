package TicketCodeIA.controller;

import TicketCodeIA.service.SseService;
import TicketCodeIA.service.AgentLogService;
import TicketCodeIA.entity.AgentLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class SseController {

    private final SseService sseService;
    private final AgentLogService agentLogService;

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        return sseService.subscribe();
    }

    @GetMapping("/recent-activity")
    public List<AgentLog> getRecentActivity() {
        return agentLogService.getRecentLogs();
    }
}
