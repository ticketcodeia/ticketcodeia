package TicketCodeIA.application.usecase.ticket;

import TicketCodeIA.application.port.out.EventPublisherPort;
import TicketCodeIA.application.query.TicketResult;
import TicketCodeIA.domain.enums.Priority;
import TicketCodeIA.domain.enums.TicketStatus;
import TicketCodeIA.domain.exception.TicketNotFoundException;
import TicketCodeIA.domain.model.ticket.Ticket;
import TicketCodeIA.domain.port.out.AgentLogRepositoryPort;
import TicketCodeIA.domain.port.out.TicketRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MoveTicketOnHumanBoardUseCaseTest {

    @Mock private TicketRepositoryPort ticketRepository;
    @Mock private AgentLogRepositoryPort agentLogRepository;
    @Mock private EventPublisherPort eventPublisher;
    @InjectMocks private MoveTicketOnHumanBoardUseCase useCase;

    private Ticket escalatedTicket() {
        Ticket ticket = Ticket.create("Title", "Desc", Priority.HIGH, null, null);
        ticket.setId(1L);
        ticket.escalate("Max retries");
        return ticket;
    }

    @Test
    void moveToHumanBoard_success() {
        Ticket ticket = escalatedTicket();
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TicketResult result = useCase.moveToHumanBoard(1L);

        assertThat(result.status()).isEqualTo(TicketStatus.HUMAN_TODO);
        verify(agentLogRepository).save(any());
        verify(eventPublisher).publish(any());
    }

    @Test
    void moveToHumanBoard_ticketNotFound_throws() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.moveToHumanBoard(99L))
                .isInstanceOf(TicketNotFoundException.class);
    }

    @Test
    void advanceStatus_toHumanDev() {
        Ticket ticket = escalatedTicket();
        ticket.moveToHumanBoard();
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TicketResult result = useCase.advanceStatus(1L, TicketStatus.HUMAN_DEV);

        assertThat(result.status()).isEqualTo(TicketStatus.HUMAN_DEV);
    }

    @Test
    void advanceStatus_toHumanReview() {
        Ticket ticket = escalatedTicket();
        ticket.moveToHumanBoard();
        ticket.startHumanDevelopment();
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TicketResult result = useCase.advanceStatus(1L, TicketStatus.HUMAN_REVIEW);

        assertThat(result.status()).isEqualTo(TicketStatus.HUMAN_REVIEW);
    }

    @Test
    void advanceStatus_toDone() {
        Ticket ticket = escalatedTicket();
        ticket.moveToHumanBoard();
        ticket.startHumanDevelopment();
        ticket.completeHumanDevelopment();
        ticket.completeHumanReview();
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TicketResult result = useCase.advanceStatus(1L, TicketStatus.DONE);

        assertThat(result.status()).isEqualTo(TicketStatus.DONE);
    }

    @Test
    void advanceStatus_invalidTarget_throws() {
        Ticket ticket = escalatedTicket();
        ticket.moveToHumanBoard();
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> useCase.advanceStatus(1L, TicketStatus.TODO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
