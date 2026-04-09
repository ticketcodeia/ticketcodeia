package TicketCodeIA.domain.model.agent;

import TicketCodeIA.domain.enums.AgentType;
import TicketCodeIA.domain.enums.TicketStatus;
import TicketCodeIA.domain.model.ticket.Ticket;
import TicketCodeIA.domain.valueobject.AgentResult;

/**
 * Domain aggregate representing the Developer agent.
 * Encapsulates the domain rules for when a developer can work on a ticket
 * and how the ticket state changes based on development results.
 */
public class DeveloperAgent extends Agent {

    public DeveloperAgent() {
        super(AgentType.DEVELOPER);
    }

    @Override
    public boolean canProcess(Ticket ticket) {
        return ticket.getStatus() == TicketStatus.TODO
                || ticket.getStatus() == TicketStatus.IN_PROGRESS;
    }

    @Override
    public void prepareTicket(Ticket ticket) {
        ticket.startDevelopment();
    }

    @Override
    public boolean applyResult(Ticket ticket, AgentResult result, boolean enableCodeReview, boolean enableTesting) {
        if (result.isFailure()) {
            ticket.escalateToHuman(getType(), "Development failed: " + result.getMessage());
            return false;
        }

        ticket.addLog("Claude CLI output: " + result.getMessage());

        if (!enableCodeReview && !enableTesting) {
            ticket.markDone("Skipped review and testing (both disabled)");
            return false;
        }

        if (!enableCodeReview) {
            ticket.skipToTesting();
        } else {
            ticket.completeDevelopment();
        }

        ticket.setBranchName("feature/ticket-" + ticket.getId());
        return true;
    }
}
