package TicketCodeIA.infrastructure.event;

import TicketCodeIA.application.port.out.EventPublisherPort;
import TicketCodeIA.domain.enums.AgentType;
import TicketCodeIA.domain.enums.TicketStatus;
import TicketCodeIA.domain.event.*;
import TicketCodeIA.infrastructure.sse.SseEvent;
import TicketCodeIA.infrastructure.sse.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SpringEventPublisherAdapter implements EventPublisherPort {

    private final SseService sseService;

    @Override
    public void publish(DomainEvent event) {
        if (event instanceof TicketStatusChangedEvent e) {
            sseService.broadcast(SseEvent.ticketUpdated(
                    e.ticketId(), e.newStatus(), e.agentType(), e.message()));
        } else if (event instanceof TicketEscalatedEvent e) {
            sseService.broadcast(SseEvent.ticketUpdated(
                    e.ticketId(), TicketStatus.ESCALATED, AgentType.HUMAN, "Escalated: " + e.reason()));
        } else if (event instanceof TicketCompletedEvent e) {
            sseService.broadcast(SseEvent.ticketUpdated(
                    e.ticketId(), TicketStatus.DONE, AgentType.DEVELOPER, "Completed: " + e.reason()));
        } else {
            log.warn("Unknown domain event type: {}", event.getClass().getSimpleName());
        }
    }
}
