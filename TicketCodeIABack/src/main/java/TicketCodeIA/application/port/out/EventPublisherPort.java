package TicketCodeIA.application.port.out;

import TicketCodeIA.domain.event.DomainEvent;

public interface EventPublisherPort {
    void publish(DomainEvent event);
}
