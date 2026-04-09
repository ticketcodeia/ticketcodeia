package TicketCodeIA.domain.model.agent;

import TicketCodeIA.domain.enums.AgentType;
import TicketCodeIA.domain.enums.TicketStatus;
import TicketCodeIA.domain.model.ticket.Ticket;
import TicketCodeIA.domain.valueobject.AgentResult;

/**
 * Domain aggregate representing the Reviewer agent.
 * Encapsulates the domain rules for code review decisions
 * and how they affect ticket state transitions.
 */
public class ReviewerAgent extends Agent {

    public ReviewerAgent() {
        super(AgentType.REVIEWER);
    }

    @Override
    public boolean canProcess(Ticket ticket) {
        return ticket.getStatus() == TicketStatus.CODE_REVIEW;
    }

    @Override
    public void prepareTicket(Ticket ticket) {
        ticket.assignReviewer();
    }

    @Override
    public boolean applyResult(Ticket ticket, AgentResult result, boolean enableCodeReview, boolean enableTesting) {
        if (result.isFailure()) {
            ticket.escalateToHuman(getType(), "Review failed: " + result.getMessage());
            return false;
        }

        if (result.needsChanges()) {
            ticket.requestChanges(result.getMessage());
            return true; // continue pipeline — goes back to developer
        }

        // Approved
        if (!enableTesting) {
            ticket.markDone("Testing disabled, marking done after review approval");
            return false;
        }

        ticket.approveReview();
        return true;
    }
}
