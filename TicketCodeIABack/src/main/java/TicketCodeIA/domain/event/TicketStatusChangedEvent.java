package TicketCodeIA.domain.event;

import TicketCodeIA.domain.enums.AgentType;
import TicketCodeIA.domain.enums.TicketStatus;

import java.time.LocalDateTime;

public record TicketStatusChangedEvent(
        Long ticketId,
        TicketStatus previousStatus,
        TicketStatus newStatus,
        AgentType agentType,
        String message,
        LocalDateTime occurredAt
) implements DomainEvent {

    public TicketStatusChangedEvent(Long ticketId, TicketStatus previousStatus,
                                    TicketStatus newStatus, AgentType agentType, String message) {
        this(ticketId, previousStatus, newStatus, agentType, message, LocalDateTime.now());
    }
}
