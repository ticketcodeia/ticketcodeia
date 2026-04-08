package TicketCodeIA.domain.model.agent;

import TicketCodeIA.domain.enums.AgentType;
import TicketCodeIA.domain.model.ticket.Ticket;
import TicketCodeIA.domain.valueobject.AgentResult;

/**
 * Base aggregate for all agents in the domain.
 * Encapsulates the identity, type, and common behavior of an agent.
 */
public abstract class Agent {

    private final AgentType type;

    protected Agent(AgentType type) {
        this.type = type;
    }

    public AgentType getType() {
        return type;
    }

    /**
     * Validates whether this agent can process the given ticket in its current state.
     */
    public abstract boolean canProcess(Ticket ticket);

    /**
     * Prepares the ticket for processing by this agent (assigns agent, transitions state).
     * This encapsulates the domain rules for what happens when an agent starts working.
     */
    public abstract void prepareTicket(Ticket ticket);

    /**
     * Applies the result of processing back onto the ticket (domain state transitions).
     * Returns true if the pipeline should continue, false if it should stop.
     */
    public abstract boolean applyResult(Ticket ticket, AgentResult result, boolean enableCodeReview, boolean enableTesting);
}
