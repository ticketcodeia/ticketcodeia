package TicketCodeIA.application.usecase.ticket;

import TicketCodeIA.application.port.out.EventPublisherPort;
import TicketCodeIA.domain.enums.AgentType;
import TicketCodeIA.domain.event.TicketCompletedEvent;
import TicketCodeIA.domain.event.TicketEscalatedEvent;
import TicketCodeIA.domain.event.TicketStatusChangedEvent;
import TicketCodeIA.domain.model.agent.Agent;
import TicketCodeIA.domain.model.agent.DeveloperAgent;
import TicketCodeIA.domain.model.agent.ReviewerAgent;
import TicketCodeIA.domain.model.agent.TesterAgent;
import TicketCodeIA.domain.model.agentlog.AgentLog;
import TicketCodeIA.domain.model.ticket.Ticket;
import TicketCodeIA.domain.enums.TicketStatus;
import TicketCodeIA.domain.port.in.DeveloperAgentPort;
import TicketCodeIA.domain.port.in.ReviewerAgentPort;
import TicketCodeIA.domain.port.in.TesterAgentPort;
import TicketCodeIA.domain.port.out.AgentLogRepositoryPort;
import TicketCodeIA.domain.port.out.TicketRepositoryPort;
import TicketCodeIA.domain.service.AgentPipelineService;
import TicketCodeIA.domain.valueobject.AgentResult;
import TicketCodeIA.domain.valueobject.ProjectContext;
import TicketCodeIA.domain.model.project.Project;
import TicketCodeIA.domain.port.out.ProjectRepositoryPort;
import TicketCodeIA.infrastructure.config.AgentConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessTicketUseCase {

    private final TicketRepositoryPort ticketRepository;
    private final ProjectRepositoryPort projectRepository;
    private final AgentLogRepositoryPort agentLogRepository;
    private final DeveloperAgentPort developerAgentPort;
    private final ReviewerAgentPort reviewerAgentPort;
    private final TesterAgentPort testerAgentPort;
    private final EventPublisherPort eventPublisher;
    private final AgentConfig agentConfig;

    private final AgentPipelineService pipelineService = new AgentPipelineService();

    @Async
    public void executeAsync(Long ticketId, boolean enableCodeReview, boolean enableTesting) {
        try {
            execute(ticketId, enableCodeReview, enableTesting);
        } catch (Exception e) {
            log.error("Error processing ticket {} asynchronously", ticketId, e);
        }
    }

    public void execute(Long ticketId, boolean enableCodeReview, boolean enableTesting) {
        log.info("Orchestrator: Starting pipeline for ticket {} (codeReview={}, testing={})",
                ticketId, enableCodeReview, enableTesting);

        Ticket ticket = loadTicket(ticketId);

        if (ticket.isInFinalState()) {
            log.info("Ticket {} is already in final state: {}", ticketId, ticket.getStatus());
            return;
        }

        String modeMsg = buildModeMessage(enableCodeReview, enableTesting);
        logAgent(ticketId, AgentType.HUMAN, "PIPELINE_STARTED", "Pipeline started - " + modeMsg);
        eventPublisher.publish(new TicketStatusChangedEvent(ticketId, ticket.getStatus(),
                ticket.getStatus(), AgentType.HUMAN, "Pipeline started - " + modeMsg));

        int cycles = 0;

        while (cycles < agentConfig.getMaxRetries()) {
            ticket = loadTicket(ticketId);

            // Ask the domain service which agent should act next
            Optional<Agent> nextAgent = pipelineService.resolveNextAgent(ticket, enableCodeReview, enableTesting);

            if (nextAgent.isEmpty()) {
                // Domain decided the pipeline is done (ticket in final state or skipped)
                if (ticket.isInFinalState()) {
                    ticketRepository.save(ticket);
                    publishCompletionEvent(ticket);
                }
                return;
            }

            Agent agent = nextAgent.get();
            TicketStatus previousStatus = ticket.getStatus();

            // Let the domain aggregate prepare the ticket (state transition + agent assignment)
            agent.prepareTicket(ticket);
            ticket = ticketRepository.save(ticket);

            logAgent(ticketId, agent.getType(), "STARTED", "Processing: " + ticket.getTitle());
            eventPublisher.publish(new TicketStatusChangedEvent(ticketId, previousStatus,
                    ticket.getStatus(), agent.getType(), agent.getType() + " started"));

            // Build project context so the agent understands the full picture
            ProjectContext projectContext = buildProjectContext(ticket);

            // Delegate to infrastructure adapter for the actual AI/CLI work
            AgentResult result = executeAgentPort(agent, ticket, projectContext);

            // Reload ticket and let the domain aggregate apply the result
            ticket = loadTicket(ticketId);
            TicketStatus statusBeforeApply = ticket.getStatus();
            boolean shouldContinue = agent.applyResult(ticket, result, enableCodeReview, enableTesting);
            ticket = ticketRepository.save(ticket);

            // Check if retries exceeded after a changes-requested or test-failure cycle
            if (shouldContinue && pipelineService.shouldEscalate(ticket, agentConfig.getMaxRetries())) {
                doEscalate(ticket, "Max retry cycles exceeded");
                return;
            }

            logAgent(ticketId, agent.getType(), result.isSuccess() ? "COMPLETED" : "RESULT",
                    result.getMessage() != null ? result.getMessage() : "Done");
            eventPublisher.publish(new TicketStatusChangedEvent(ticketId, statusBeforeApply,
                    ticket.getStatus(), agent.getType(), result.getMessage()));

            if (!shouldContinue) {
                publishCompletionEvent(ticket);
                return;
            }

            if (result.needsChanges() || result.isFailure()) {
                cycles++;
            }

            ticket = loadTicket(ticketId);
            if (ticket.isInFinalState()) {
                return;
            }
        }

        doEscalate(loadTicket(ticketId), "Max retry cycles exceeded");
    }

    public void saveAgentFlags(Long ticketId, boolean enableCodeReview, boolean enableTesting) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));
        ticket.setEnableCodeReview(enableCodeReview);
        ticket.setEnableTesting(enableTesting);
        ticketRepository.save(ticket);
    }

    private AgentResult executeAgentPort(Agent agent, Ticket ticket, ProjectContext context) {
        if (agent instanceof DeveloperAgent) {
            return developerAgentPort.process(ticket, context);
        } else if (agent instanceof ReviewerAgent) {
            return reviewerAgentPort.process(ticket, context);
        } else if (agent instanceof TesterAgent) {
            return testerAgentPort.process(ticket, context);
        }
        return AgentResult.failure("Unknown agent type: " + agent.getType());
    }

    private ProjectContext buildProjectContext(Ticket ticket) {
        String projectName = ticket.getProjectName() != null ? ticket.getProjectName() : "Unknown";
        String projectDescription = "";

        if (ticket.getProjectId() != null) {
            projectDescription = projectRepository.findById(ticket.getProjectId())
                    .map(Project::getDescription)
                    .orElse("");
        }

        List<ProjectContext.TicketSummary> allTickets = List.of();
        if (ticket.getProjectId() != null) {
            allTickets = ticketRepository.findByProjectIdOrderByCreatedAtDesc(ticket.getProjectId())
                    .stream()
                    .map(ProjectContext.TicketSummary::fromTicket)
                    .toList();
        }

        return new ProjectContext(projectName, projectDescription, allTickets);
    }

    private void publishCompletionEvent(Ticket ticket) {
        if (ticket.getStatus() == TicketStatus.DONE) {
            logAgent(ticket.getId(), AgentType.HUMAN, "PIPELINE_COMPLETED", "Ticket completed successfully");
            eventPublisher.publish(new TicketCompletedEvent(ticket.getId(), "Ticket completed successfully"));
        } else if (ticket.getStatus() == TicketStatus.ESCALATED || ticket.isOnHumanBoard()) {
            logAgent(ticket.getId(), AgentType.HUMAN, "ESCALATED_TO_HUMAN", "Ticket escalated to human board");
            eventPublisher.publish(new TicketEscalatedEvent(ticket.getId(), "Ticket escalated to human board"));
        }
    }

    private Ticket loadTicket(Long ticketId) {
        return ticketRepository.findByIdWithProject(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));
    }

    private void doEscalate(Ticket ticket, String reason) {
        log.warn("Orchestrator: Escalating ticket {} - {}", ticket.getId(), reason);
        AgentType failingAgent = ticket.getAssignedAgent() != null ? ticket.getAssignedAgent() : AgentType.HUMAN;
        ticket.escalateToHuman(failingAgent, reason);
        ticketRepository.save(ticket);
        logAgent(ticket.getId(), AgentType.HUMAN, "ESCALATED_TO_HUMAN", reason);
        eventPublisher.publish(new TicketEscalatedEvent(ticket.getId(), reason));
    }

    private void logAgent(Long ticketId, AgentType agentType, String action, String message) {
        agentLogRepository.save(AgentLog.create(ticketId, agentType, action, message));
    }

    private String buildModeMessage(boolean enableCodeReview, boolean enableTesting) {
        if (enableCodeReview && enableTesting) return "full pipeline (dev -> review -> test)";
        if (enableCodeReview) return "dev + review only (testing skipped)";
        if (enableTesting) return "dev + testing only (review skipped)";
        return "dev only (review and testing skipped)";
    }
}
