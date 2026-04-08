package TicketCodeIA.domain.service;

import TicketCodeIA.domain.enums.TicketStatus;
import TicketCodeIA.domain.model.agent.Agent;
import TicketCodeIA.domain.model.agent.DeveloperAgent;
import TicketCodeIA.domain.model.agent.ReviewerAgent;
import TicketCodeIA.domain.model.agent.TesterAgent;
import TicketCodeIA.domain.model.ticket.Ticket;

import java.util.Optional;

/**
 * Domain service that encapsulates the pipeline orchestration logic.
 * Determines which agent should process a ticket next based on its current state
 * and the enabled pipeline stages.
 */
public class AgentPipelineService {

    private final DeveloperAgent developerAgent = new DeveloperAgent();
    private final ReviewerAgent reviewerAgent = new ReviewerAgent();
    private final TesterAgent testerAgent = new TesterAgent();

    /**
     * Resolves the next agent that should process the ticket based on its current state
     * and pipeline configuration.
     */
    public Optional<Agent> resolveNextAgent(Ticket ticket, boolean enableCodeReview, boolean enableTesting) {
        if (ticket.isInFinalState()) {
            return Optional.empty();
        }

        TicketStatus status = ticket.getStatus();

        if (status == TicketStatus.TODO || status == TicketStatus.IN_PROGRESS) {
            return Optional.of(developerAgent);
        }

        if (status == TicketStatus.CODE_REVIEW) {
            if (!enableCodeReview) {
                // Skip review — move directly based on testing flag
                if (enableTesting) {
                    ticket.approveReview();
                    return Optional.of(testerAgent);
                } else {
                    ticket.markDone("Review and testing disabled");
                    return Optional.empty();
                }
            }
            return Optional.of(reviewerAgent);
        }

        if (status == TicketStatus.TESTING) {
            if (!enableTesting) {
                ticket.markDone("Testing disabled");
                return Optional.empty();
            }
            return Optional.of(testerAgent);
        }

        return Optional.empty();
    }

    /**
     * Checks if the pipeline should escalate due to exceeded retries.
     */
    public boolean shouldEscalate(Ticket ticket, int maxRetries) {
        return ticket.hasExceededRetries(maxRetries);
    }

    public DeveloperAgent getDeveloperAgent() { return developerAgent; }
    public ReviewerAgent getReviewerAgent() { return reviewerAgent; }
    public TesterAgent getTesterAgent() { return testerAgent; }
}
