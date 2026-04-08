package TicketCodeIA.domain.port.in;

import TicketCodeIA.domain.model.ticket.Ticket;
import TicketCodeIA.domain.valueobject.AgentResult;

public interface TesterAgentPort {
    AgentResult process(Ticket ticket);
}
