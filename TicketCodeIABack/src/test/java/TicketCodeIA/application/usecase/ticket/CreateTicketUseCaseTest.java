package TicketCodeIA.application.usecase.ticket;

import TicketCodeIA.application.command.CreateTicketCommand;
import TicketCodeIA.application.query.TicketResult;
import TicketCodeIA.domain.enums.Priority;
import TicketCodeIA.domain.enums.TicketStatus;
import TicketCodeIA.domain.model.ticket.Ticket;
import TicketCodeIA.domain.port.out.ProjectRepositoryPort;
import TicketCodeIA.domain.port.out.TicketRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateTicketUseCaseTest {

    @Mock private TicketRepositoryPort ticketRepository;
    @Mock private ProjectRepositoryPort projectRepository;
    @InjectMocks private CreateTicketUseCase useCase;

    @Test
    void execute_createsAndReturnsTicket() {
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        var command = new CreateTicketCommand("Title", "Desc", Priority.HIGH, null);
        TicketResult result = useCase.execute(command);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.title()).isEqualTo("Title");
        assertThat(result.status()).isEqualTo(TicketStatus.TODO);
        assertThat(result.priority()).isEqualTo(Priority.HIGH);
    }
}
