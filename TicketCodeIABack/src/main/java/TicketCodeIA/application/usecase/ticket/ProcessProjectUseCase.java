package TicketCodeIA.application.usecase.ticket;

import TicketCodeIA.domain.enums.TicketStatus;
import TicketCodeIA.domain.model.ticket.Ticket;
import TicketCodeIA.domain.port.in.POAgentPort;
import TicketCodeIA.domain.port.out.TicketRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessProjectUseCase {

    private static final List<TicketStatus> ACTIVE_STATUSES = List.of(
            TicketStatus.IN_PROGRESS, TicketStatus.CODE_REVIEW, TicketStatus.TESTING
    );

    private final TicketRepositoryPort ticketRepository;
    private final ProcessTicketUseCase processTicketUseCase;
    private final POAgentPort poAgentPort;

    @Async
    public void executeAsync(Long projectId) {
        try {
            execute(projectId);
        } catch (Exception e) {
            log.error("Error processing project {} tickets", projectId, e);
        }
    }

    public void execute(Long projectId) {
        log.info("ProcessProject: Starting for project {}", projectId);

        List<Ticket> todoTickets = ticketRepository.findByProjectIdAndStatus(projectId, TicketStatus.TODO);

        if (todoTickets.isEmpty()) {
            log.info("ProcessProject: No TODO tickets for project {}", projectId);
            return;
        }

        int totalTodo = todoTickets.size();
        for (int i = 0; i < totalTodo; i++) {
            // Wait until the pipeline columns are clear
            waitForPipelineClear(projectId);

            // Reload current TODO tickets (some may have been processed already)
            List<Ticket> remainingTodo = ticketRepository.findByProjectIdAndStatus(projectId, TicketStatus.TODO);
            if (remainingTodo.isEmpty()) {
                log.info("ProcessProject: No more TODO tickets");
                break;
            }

            // PO agent chooses the best next ticket
            Long chosenId = chooseNextTicket(projectId, remainingTodo);

            // Re-check the chosen ticket is still TODO
            Ticket fresh = ticketRepository.findById(chosenId).orElse(null);
            if (fresh == null || fresh.getStatus() != TicketStatus.TODO) {
                log.info("ProcessProject: Chosen ticket {} is no longer TODO, skipping", chosenId);
                continue;
            }

            boolean enableCodeReview = fresh.isEnableCodeReview();
            boolean enableTesting = fresh.isEnableTesting();

            log.info("ProcessProject: PO chose ticket {} - {} (codeReview={}, testing={})",
                    fresh.getId(), fresh.getTitle(), enableCodeReview, enableTesting);
            processTicketUseCase.execute(fresh.getId(), enableCodeReview, enableTesting);
        }

        log.info("ProcessProject: Completed all TODO tickets for project {}", projectId);
    }

    private Long chooseNextTicket(Long projectId, List<Ticket> remainingTodo) {
        // Build summary of all project tickets (just title + status)
        List<Ticket> allTickets = ticketRepository.findByProjectId(projectId);
        List<Map<String, String>> allTicketsSummary = allTickets.stream()
                .map(t -> {
                    Map<String, String> m = new HashMap<>();
                    m.put("title", t.getTitle());
                    m.put("status", t.getStatus().name());
                    return m;
                }).toList();

        // Build TODO tickets list (id + title)
        List<Map<String, Object>> todoList = remainingTodo.stream()
                .map(t -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", t.getId());
                    m.put("title", t.getTitle());
                    return m;
                }).toList();

        return poAgentPort.chooseNextTicket(allTicketsSummary, todoList);
    }

    private void waitForPipelineClear(Long projectId) {
        int maxWaitSeconds = 600;
        int waited = 0;

        while (waited < maxWaitSeconds) {
            long activeCount = ticketRepository.countByProjectIdAndStatusIn(projectId, ACTIVE_STATUSES);
            if (activeCount == 0) {
                return;
            }
            try {
                Thread.sleep(2000);
                waited += 2;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        log.warn("ProcessProject: Timeout waiting for pipeline to clear for project {}", projectId);
    }
}
