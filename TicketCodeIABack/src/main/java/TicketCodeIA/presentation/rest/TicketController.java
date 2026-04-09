package TicketCodeIA.presentation.rest;

import TicketCodeIA.application.command.CreateTicketCommand;
import TicketCodeIA.application.command.UpdateTicketCommand;
import TicketCodeIA.application.query.TicketQuery;
import TicketCodeIA.application.query.TicketResult;
import TicketCodeIA.application.usecase.agentlog.GetAgentLogsUseCase;
import TicketCodeIA.application.usecase.ticket.*;
import TicketCodeIA.domain.enums.TicketStatus;
import TicketCodeIA.presentation.dto.request.CreateTicketRequest;
import TicketCodeIA.presentation.dto.request.ProcessTicketRequest;
import TicketCodeIA.presentation.dto.request.UpdateTicketRequest;
import TicketCodeIA.presentation.dto.response.AgentLogResponse;
import TicketCodeIA.presentation.dto.response.TicketResponse;
import TicketCodeIA.presentation.dto.response.TicketStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final CreateTicketUseCase createTicketUseCase;
    private final GetTicketUseCase getTicketUseCase;
    private final UpdateTicketUseCase updateTicketUseCase;
    private final GetTicketStatsUseCase getTicketStatsUseCase;
    private final ProcessTicketUseCase processTicketUseCase;
    private final GetAgentLogsUseCase getAgentLogsUseCase;
    private final MoveTicketOnHumanBoardUseCase moveTicketOnHumanBoardUseCase;
    private final ProcessProjectUseCase processProjectUseCase;

    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(@RequestBody CreateTicketRequest request) {
        var command = new CreateTicketCommand(
                request.getTitle(), request.getDescription(), request.getPriority(), null);
        TicketResult result = createTicketUseCase.execute(command);
        return ResponseEntity.ok(TicketResponse.fromResult(result));
    }

    @GetMapping
    public ResponseEntity<List<TicketResponse>> getAllTickets(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) TicketStatus status) {
        var query = new TicketQuery(projectId, status);
        List<TicketResponse> tickets = getTicketUseCase.getAll(query).stream()
                .map(TicketResponse::fromResult).toList();
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicketById(@PathVariable Long id) {
        TicketResult result = getTicketUseCase.getById(id);
        return ResponseEntity.ok(TicketResponse.fromResult(result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TicketResponse> updateTicket(
            @PathVariable Long id, @RequestBody UpdateTicketRequest request) {
        var command = new UpdateTicketCommand(
                id, request.getTitle(), request.getDescription(),
                request.getPriority(), request.getStatus());
        TicketResult result = updateTicketUseCase.execute(command);
        return ResponseEntity.ok(TicketResponse.fromResult(result));
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<TicketResponse> processTicket(
            @PathVariable Long id,
            @RequestBody(required = false) ProcessTicketRequest request) {
        if (request == null) {
            request = new ProcessTicketRequest();
        }
        processTicketUseCase.saveAgentFlags(id, request.isEnableCodeReview(), request.isEnableTesting());
        processTicketUseCase.executeAsync(id, request.isEnableCodeReview(), request.isEnableTesting());
        TicketResult result = getTicketUseCase.getById(id);
        return ResponseEntity.accepted().body(TicketResponse.fromResult(result));
    }

    @PostMapping("/process-project")
    public ResponseEntity<Void> processProject(@RequestParam Long projectId) {
        processProjectUseCase.executeAsync(projectId);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{id}/move-to-human-board")
    public ResponseEntity<TicketResponse> moveToHumanBoard(@PathVariable Long id) {
        TicketResult result = moveTicketOnHumanBoardUseCase.moveToHumanBoard(id);
        return ResponseEntity.ok(TicketResponse.fromResult(result));
    }

    @PutMapping("/{id}/human-board-status")
    public ResponseEntity<TicketResponse> advanceHumanBoardStatus(
            @PathVariable Long id, @RequestParam TicketStatus status) {
        TicketResult result = moveTicketOnHumanBoardUseCase.advanceStatus(id, status);
        return ResponseEntity.ok(TicketResponse.fromResult(result));
    }

    @GetMapping("/stats")
    public ResponseEntity<TicketStatsResponse> getStats() {
        return ResponseEntity.ok(TicketStatsResponse.fromResult(getTicketStatsUseCase.execute()));
    }

    @GetMapping("/{id}/logs")
    public ResponseEntity<List<AgentLogResponse>> getTicketLogs(@PathVariable Long id) {
        List<AgentLogResponse> logs = getAgentLogsUseCase.getByTicketId(id).stream()
                .map(AgentLogResponse::fromResult).toList();
        return ResponseEntity.ok(logs);
    }
}
