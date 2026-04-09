package TicketCodeIA.application.usecase.ticket;

import TicketCodeIA.domain.exception.TicketNotFoundException;
import TicketCodeIA.domain.port.out.TicketRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteTicketUseCase {

    private final TicketRepositoryPort ticketRepository;

    @Transactional
    public void execute(Long id) {
        ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        ticketRepository.deleteById(id);
    }
}
