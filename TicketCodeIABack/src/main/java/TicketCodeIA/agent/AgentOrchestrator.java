package TicketCodeIA.agent;

import TicketCodeIA.dto.SseEvent;
import TicketCodeIA.entity.Ticket;
import TicketCodeIA.enums.AgentType;
import TicketCodeIA.enums.TicketStatus;
import TicketCodeIA.service.AgentLogService;
import TicketCodeIA.service.SseService;
import TicketCodeIA.service.TicketService;
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
    public void processTicketAsync(Long ticketId, boolean enableCodeReview, boolean enableTesting) {
        try {
            processTicket(ticketId, enableCodeReview, enableTesting);
        } catch (Exception e) {
            log.error("Error processing ticket {} asynchronously", ticketId, e);
        }
    }

    public void processTicket(Long ticketId, boolean enableCodeReview, boolean enableTesting) {
        log.info("Orchestrator: Starting pipeline for ticket {} (codeReview={}, testing={})",
                ticketId, enableCodeReview, enableTesting);

        Ticket ticket = ticketService.getTicketEntity(ticketId);

        if (ticket.getStatus() == TicketStatus.DONE || ticket.getStatus() == TicketStatus.ESCALATED) {
            log.info("Ticket {} is already in final state: {}", ticketId, ticket.getStatus());
            return;
        }

        String modeMsg = buildModeMessage(enableCodeReview, enableTesting);
        agentLogService.log(ticketId, AgentType.HUMAN, "PIPELINE_STARTED",
                "Pipeline started — " + modeMsg);

        sseService.broadcast(SseEvent.ticketUpdated(ticketId, ticket.getStatus(), AgentType.HUMAN,
                "Pipeline started — " + modeMsg));

        int developmentCycles = 0;

        while (developmentCycles < maxRetries) {
            ticket = ticketService.getTicketEntity(ticketId);

            switch (ticket.getStatus()) {

                case TODO:
                case IN_PROGRESS: {
                    AgentResult devResult = developerAgent.process(ticket);
                    if (devResult.isFailure()) {
                        escalateTicket(ticket, "Development failed: " + devResult.getMessage());
                        return;
                    }
                    // After dev, decide next step based on enabled agents
                    ticket = ticketService.getTicketEntity(ticketId);
                    if (!enableCodeReview && !enableTesting) {
                        markDone(ticket, "Skipped review and testing (both disabled)");
                        return;
                    }
                    if (!enableCodeReview) {
                        // Skip review, go straight to testing
                        ticket.setStatus(TicketStatus.TESTING);
                        ticketService.saveTicket(ticket);
                        sseService.broadcast(SseEvent.ticketUpdated(ticketId, TicketStatus.TESTING,
                                AgentType.DEVELOPER, "Code review skipped, moving to testing"));
                    }
                    break;
                }

                case CODE_REVIEW: {
                    if (!enableCodeReview) {
                        // Should not reach here, but handle gracefully
                        ticket.setStatus(enableTesting ? TicketStatus.TESTING : TicketStatus.DONE);
                        ticketService.saveTicket(ticket);
                        break;
                    }
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
                        sseService.broadcast(SseEvent.ticketUpdated(ticketId, TicketStatus.IN_PROGRESS,
                                AgentType.REVIEWER, "Changes requested, retry " + developmentCycles));
                    } else {
                        // Approved — check if testing is enabled
                        if (!enableTesting) {
                            ticket = ticketService.getTicketEntity(ticketId);
                            markDone(ticket, "Testing disabled, marking done after review approval");
                            return;
                        }
                    }
                    break;
                }

                case TESTING: {
                    if (!enableTesting) {
                        ticket = ticketService.getTicketEntity(ticketId);
                        markDone(ticket, "Testing disabled");
                        return;
                    }
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
                        sseService.broadcast(SseEvent.ticketUpdated(ticketId, TicketStatus.IN_PROGRESS,
                                AgentType.TESTER, "Tests failed, retry " + developmentCycles));
                    }
                    break;
                }

                case DONE:
                case ESCALATED:
                    return;
            }

            ticket = ticketService.getTicketEntity(ticketId);
            if (ticket.getStatus() == TicketStatus.DONE || ticket.getStatus() == TicketStatus.ESCALATED) {
                return;
            }
        }

        escalateTicket(ticketService.getTicketEntity(ticketId), "Max retry cycles exceeded");
    }

    private void markDone(Ticket ticket, String reason) {
        ticket.setStatus(TicketStatus.DONE);
        ticket.setAssignedAgent(AgentType.DEVELOPER);
        ticket.addAgentLog("Marked DONE: " + reason);
        ticketService.saveTicket(ticket);
        agentLogService.log(ticket.getId(), AgentType.HUMAN, "PIPELINE_COMPLETED", reason);
        sseService.broadcast(SseEvent.ticketUpdated(ticket.getId(), TicketStatus.DONE,
                AgentType.DEVELOPER, "Ticket completed — " + reason));
    }

    private void escalateTicket(Ticket ticket, String reason) {
        log.warn("Orchestrator: Escalating ticket {} — {}", ticket.getId(), reason);
        ticket.setStatus(TicketStatus.ESCALATED);
        ticket.setAssignedAgent(AgentType.HUMAN);
        ticket.addAgentLog("ESCALATED: " + reason);
        ticketService.saveTicket(ticket);
        agentLogService.log(ticket.getId(), AgentType.HUMAN, "ESCALATED", reason);
        sseService.broadcast(SseEvent.ticketUpdated(ticket.getId(), TicketStatus.ESCALATED,
                AgentType.HUMAN, "Escalated: " + reason));
    }

    private String buildModeMessage(boolean enableCodeReview, boolean enableTesting) {
        if (enableCodeReview && enableTesting) return "full pipeline (dev → review → test)";
        if (enableCodeReview) return "dev + review only (testing skipped)";
        if (enableTesting) return "dev + testing only (review skipped)";
        return "dev only (review and testing skipped)";
    }
}
