package TicketCodeIA.application.usecase.ticket;

import TicketCodeIA.application.query.TicketStatsResult;
import TicketCodeIA.domain.enums.TicketStatus;
import TicketCodeIA.domain.port.out.TicketRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetTicketStatsUseCase {

    private final TicketRepositoryPort ticketRepository;

    @Transactional(readOnly = true)
    public TicketStatsResult execute() {
        return new TicketStatsResult(
                ticketRepository.count(),
                ticketRepository.countByStatus(TicketStatus.TODO),
                ticketRepository.countByStatus(TicketStatus.IN_PROGRESS),
                ticketRepository.countByStatus(TicketStatus.CODE_REVIEW),
                ticketRepository.countByStatus(TicketStatus.TESTING),
                ticketRepository.countByStatus(TicketStatus.DONE),
                ticketRepository.countByStatus(TicketStatus.ESCALATED)
        );
    }
}
