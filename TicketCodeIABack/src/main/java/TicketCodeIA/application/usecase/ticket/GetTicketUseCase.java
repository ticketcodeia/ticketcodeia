package TicketCodeIA.application.usecase.ticket;

import TicketCodeIA.application.query.TicketQuery;
import TicketCodeIA.application.query.TicketResult;
import TicketCodeIA.domain.exception.TicketNotFoundException;
import TicketCodeIA.domain.model.ticket.Ticket;
import TicketCodeIA.domain.port.out.TicketRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetTicketUseCase {

    private final TicketRepositoryPort ticketRepository;

    @Transactional(readOnly = true)
    public TicketResult getById(Long id) {
        Ticket ticket = ticketRepository.findByIdWithProject(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        return TicketResult.fromDomain(ticket);
    }

    @Transactional(readOnly = true)
    public List<TicketResult> getAll(TicketQuery query) {
        List<Ticket> tickets;

        if (query.projectId() != null && query.status() != null) {
            tickets = ticketRepository.findByProjectIdAndStatus(query.projectId(), query.status());
        } else if (query.projectId() != null) {
            tickets = ticketRepository.findByProjectIdOrderByCreatedAtDesc(query.projectId());
        } else if (query.status() != null) {
            tickets = ticketRepository.findByStatus(query.status());
        } else {
            tickets = ticketRepository.findAllOrderByCreatedAtDesc();
        }

        return tickets.stream().map(TicketResult::fromDomain).toList();
    }
}
