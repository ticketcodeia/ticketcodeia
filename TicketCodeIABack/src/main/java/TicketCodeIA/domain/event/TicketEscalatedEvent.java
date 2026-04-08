package TicketCodeIA.domain.event;

import java.time.LocalDateTime;

public record TicketEscalatedEvent(
        Long ticketId,
        String reason,
        LocalDateTime occurredAt
) implements DomainEvent {

    public TicketEscalatedEvent(Long ticketId, String reason) {
        this(ticketId, reason, LocalDateTime.now());
    }
}
