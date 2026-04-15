package TicketCodeIA.domain.port.in;

import TicketCodeIA.domain.model.ticket.Ticket;
import TicketCodeIA.domain.valueobject.AgentResult;
import TicketCodeIA.domain.valueobject.ProjectContext;

public interface ReviewerAgentPort {
    AgentResult process(Ticket ticket, ProjectContext context);
}
