package TicketCodeIA.domain.model.agent;

import TicketCodeIA.domain.enums.AgentType;
import TicketCodeIA.domain.enums.TicketStatus;
import TicketCodeIA.domain.model.ticket.Ticket;
import TicketCodeIA.domain.valueobject.AgentResult;

/**
 * Domain aggregate representing the Tester agent.
 * Encapsulates the domain rules for test results
 * and how they affect ticket state transitions.
 */
public class TesterAgent extends Agent {

    public TesterAgent() {
        super(AgentType.TESTER);
    }

    @Override
    public boolean canProcess(Ticket ticket) {
        return ticket.getStatus() == TicketStatus.TESTING;
    }

    @Override
    public void prepareTicket(Ticket ticket) {
        ticket.assignTester();
    }

    @Override
    public boolean applyResult(Ticket ticket, AgentResult result, boolean enableCodeReview, boolean enableTesting) {
        if (result.isSuccess()) {
            ticket.completeTests();
            return false; // pipeline done
        }

        if (result.isFailure()) {
            ticket.failTests(result.getMessage());
            return true; // continue pipeline — goes back to developer
        }

        return false;
    }
}
