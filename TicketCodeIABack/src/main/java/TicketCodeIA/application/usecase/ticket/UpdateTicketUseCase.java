package TicketCodeIA.application.usecase.ticket;

import TicketCodeIA.application.command.UpdateTicketCommand;
import TicketCodeIA.application.query.TicketResult;
import TicketCodeIA.domain.exception.TicketNotFoundException;
import TicketCodeIA.domain.model.ticket.Ticket;
import TicketCodeIA.domain.port.out.TicketRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateTicketUseCase {

    private final TicketRepositoryPort ticketRepository;

    @Transactional
    public TicketResult execute(UpdateTicketCommand command) {
        Ticket ticket = ticketRepository.findById(command.id())
                .orElseThrow(() -> new TicketNotFoundException(command.id()));

        if (command.title() != null) ticket.setTitle(command.title());
        if (command.description() != null) ticket.setDescription(command.description());
        if (command.priority() != null) ticket.setPriority(command.priority());
        if (command.status() != null) ticket.setStatus(command.status());

        Ticket saved = ticketRepository.save(ticket);
        return TicketResult.fromDomain(saved);
    }
}
