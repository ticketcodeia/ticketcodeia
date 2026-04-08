package TicketCodeIA.domain.event;

import java.time.LocalDateTime;

public record TicketCompletedEvent(
        Long ticketId,
        String reason,
        LocalDateTime occurredAt
) implements DomainEvent {

    public TicketCompletedEvent(Long ticketId, String reason) {
        this(ticketId, reason, LocalDateTime.now());
    }
}
