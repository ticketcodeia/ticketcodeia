package TicketCodeIA.agent;

import TicketCodeIA.entity.Ticket;
import TicketCodeIA.enums.AgentType;
import TicketCodeIA.enums.TicketStatus;
import TicketCodeIA.service.AgentLogService;
import TicketCodeIA.service.SseService;
import TicketCodeIA.service.TicketService;
import TicketCodeIA.dto.SseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AgentOrchestrator {

    private final TicketService ticketService;
    private final DeveloperAgent developerAgent;
    private final ReviewerAgent reviewerAgent;
    private final TesterAgent testerAgent;
    private final AgentLogService agentLogService;
    private final SseService sseService;

    @Value("${tickcode.agents.max-retries:3}")
    private int maxRetries;

    @Async
    public void processTicketAsync(Long ticketId) {
        try {
            processTicket(ticketId);
        } catch (Exception e) {
            log.error("Error processing ticket {} asynchronously", ticketId, e);
        }
    }

    public void processTicket(Long ticketId) {
        log.info("Orchestrator: Starting pipeline for ticket {}", ticketId);

        Ticket ticket = ticketService.getTicketEntity(ticketId);

        if (ticket.getStatus() == TicketStatus.DONE || ticket.getStatus() == TicketStatus.ESCALATED) {
            log.info("Ticket {} is already in final state: {}", ticketId, ticket.getStatus());
            return;
        }

        agentLogService.log(ticketId, AgentType.HUMAN, "PIPELINE_STARTED",
                "Agent pipeline initiated for ticket: " + ticket.getTitle());

        sseService.broadcast(SseEvent.ticketUpdated(
                ticketId,
                ticket.getStatus(),
                AgentType.HUMAN,
                "Pipeline started"
        ));

        int developmentCycles = 0;

        while (developmentCycles < maxRetries) {
            ticket = ticketService.getTicketEntity(ticketId);

            switch (ticket.getStatus()) {
                case TODO:
                case IN_PROGRESS:
                    AgentResult devResult = developerAgent.process(ticket);
                    if (devResult.isFailure()) {
                        escalateTicket(ticket, "Development failed: " + devResult.getMessage());
                        return;
                    }
                    break;

                case CODE_REVIEW:
                    AgentResult reviewResult = reviewerAgent.process(ticket);
                    if (reviewResult.isFailure()) {
                        escalateTicket(ticket, "Review failed: " + reviewResult.getMessage());
                        return;
                    }
                    if (reviewResult.needsChanges()) {
                        developmentCycles++;
                        if (developmentCycles >= maxRetries) {
                            escalateTicket(ticket, "Max review cycles reached: " + reviewResult.getMessage());
                            return;
                        }
                        ticket = ticketService.getTicketEntity(ticketId);
                        ticket.setStatus(TicketStatus.IN_PROGRESS);
                        ticket.setRetryCount(developmentCycles);
                        ticketService.saveTicket(ticket);

                        sseService.broadcast(SseEvent.ticketUpdated(
                                ticketId,
                                TicketStatus.IN_PROGRESS,
                                AgentType.REVIEWER,
                                "Changes requested, returning to development (cycle " + developmentCycles + ")"
                        ));
                    }
                    break;

                case TESTING:
                    AgentResult testResult = testerAgent.process(ticket);
                    if (testResult.isSuccess()) {
                        log.info("Orchestrator: Ticket {} completed successfully", ticketId);
                        agentLogService.log(ticketId, AgentType.HUMAN, "PIPELINE_COMPLETED",
                                "Ticket completed successfully");
                        return;
                    }
                    if (testResult.isFailure()) {
                        developmentCycles++;
                        if (developmentCycles >= maxRetries) {
                            escalateTicket(ticket, "Max test cycles reached: " + testResult.getMessage());
                            return;
                        }
                        ticket = ticketService.getTicketEntity(ticketId);
                        ticket.setStatus(TicketStatus.IN_PROGRESS);
                        ticket.setRetryCount(developmentCycles);
                        ticketService.saveTicket(ticket);

                        sseService.broadcast(SseEvent.ticketUpdated(
                                ticketId,
                                TicketStatus.IN_PROGRESS,
                                AgentType.TESTER,
                                "Tests failed, returning to development (cycle " + developmentCycles + ")"
                        ));
                    }
                    break;

                case DONE:
                    log.info("Orchestrator: Ticket {} is already done", ticketId);
                    return;

                case ESCALATED:
                    log.info("Orchestrator: Ticket {} is escalated, stopping pipeline", ticketId);
                    return;
            }

            ticket = ticketService.getTicketEntity(ticketId);
            if (ticket.getStatus() == TicketStatus.DONE || ticket.getStatus() == TicketStatus.ESCALATED) {
                return;
            }
        }

        escalateTicket(ticketService.getTicketEntity(ticketId), "Max retry cycles exceeded");
    }

    private void escalateTicket(Ticket ticket, String reason) {
        log.warn("Orchestrator: Escalating ticket {} - {}", ticket.getId(), reason);

        ticket.setStatus(TicketStatus.ESCALATED);
        ticket.setAssignedAgent(AgentType.HUMAN);
        ticket.addAgentLog("ESCALATED: " + reason);
        ticketService.saveTicket(ticket);

        agentLogService.log(ticket.getId(), AgentType.HUMAN, "ESCALATED", reason);

        sseService.broadcast(SseEvent.ticketUpdated(
                ticket.getId(),
                TicketStatus.ESCALATED,
                AgentType.HUMAN,
                "Ticket escalated: " + reason
        ));
    }
}
