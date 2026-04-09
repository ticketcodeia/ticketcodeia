package TicketCodeIA.application.usecase.ticket;

import TicketCodeIA.application.port.out.EventPublisherPort;
import TicketCodeIA.application.query.TicketResult;
import TicketCodeIA.domain.enums.AgentType;
import TicketCodeIA.domain.enums.TicketStatus;
import TicketCodeIA.domain.event.TicketStatusChangedEvent;
import TicketCodeIA.domain.exception.TicketNotFoundException;
import TicketCodeIA.domain.model.agentlog.AgentLog;
import TicketCodeIA.domain.model.ticket.Ticket;
import TicketCodeIA.domain.port.out.AgentLogRepositoryPort;
import TicketCodeIA.domain.port.out.TicketRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MoveTicketOnHumanBoardUseCase {

    private final TicketRepositoryPort ticketRepository;
    private final AgentLogRepositoryPort agentLogRepository;
    private final EventPublisherPort eventPublisher;

    @Transactional
    public TicketResult moveToHumanBoard(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        TicketStatus previousStatus = ticket.getStatus();
        ticket.moveToHumanBoard();
        Ticket saved = ticketRepository.save(ticket);

        agentLogRepository.save(AgentLog.create(ticketId, AgentType.HUMAN, "MOVED_TO_HUMAN_BOARD",
                "Ticket moved to human board"));
        eventPublisher.publish(new TicketStatusChangedEvent(ticketId, previousStatus,
                TicketStatus.HUMAN_TODO, AgentType.HUMAN, "Moved to human board"));

        return TicketResult.fromDomain(saved);
    }

    @Transactional
    public TicketResult advanceStatus(Long ticketId, TicketStatus targetStatus) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        TicketStatus previousStatus = ticket.getStatus();

        switch (targetStatus) {
            case HUMAN_DEV -> ticket.startHumanDevelopment();
            case HUMAN_REVIEW -> ticket.completeHumanDevelopment();
            case HUMAN_TESTING -> ticket.completeHumanReview();
            case DONE -> ticket.completeHumanTesting();
            default -> throw new IllegalArgumentException("Invalid human board target status: " + targetStatus);
        }

        Ticket saved = ticketRepository.save(ticket);

        String action = "HUMAN_" + targetStatus.name();
        agentLogRepository.save(AgentLog.create(ticketId, AgentType.HUMAN, action,
                "Human moved ticket to " + targetStatus));
        eventPublisher.publish(new TicketStatusChangedEvent(ticketId, previousStatus,
                targetStatus, AgentType.HUMAN, "Human moved ticket to " + targetStatus));

        return TicketResult.fromDomain(saved);
    }
}
