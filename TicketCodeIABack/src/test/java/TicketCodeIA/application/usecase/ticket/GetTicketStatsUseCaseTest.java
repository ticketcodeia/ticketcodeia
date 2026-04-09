package TicketCodeIA.application.usecase.ticket;

import TicketCodeIA.application.query.TicketStatsResult;
import TicketCodeIA.domain.enums.TicketStatus;
import TicketCodeIA.domain.port.out.TicketRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTicketStatsUseCaseTest {

    @Mock private TicketRepositoryPort ticketRepository;
    @InjectMocks private GetTicketStatsUseCase useCase;

    @Test
    void execute_returnsAggregatedStats() {
        when(ticketRepository.count()).thenReturn(10L);
        when(ticketRepository.countByStatus(TicketStatus.TODO)).thenReturn(3L);
        when(ticketRepository.countByStatus(TicketStatus.IN_PROGRESS)).thenReturn(2L);
        when(ticketRepository.countByStatus(TicketStatus.CODE_REVIEW)).thenReturn(1L);
        when(ticketRepository.countByStatus(TicketStatus.TESTING)).thenReturn(1L);
        when(ticketRepository.countByStatus(TicketStatus.DONE)).thenReturn(2L);
        when(ticketRepository.countByStatus(TicketStatus.ESCALATED)).thenReturn(1L);
        when(ticketRepository.countByStatus(TicketStatus.HUMAN_TODO)).thenReturn(0L);
        when(ticketRepository.countByStatus(TicketStatus.HUMAN_DEV)).thenReturn(0L);
        when(ticketRepository.countByStatus(TicketStatus.HUMAN_REVIEW)).thenReturn(0L);
        when(ticketRepository.countByStatus(TicketStatus.HUMAN_TESTING)).thenReturn(0L);

        TicketStatsResult result = useCase.execute();

        assertThat(result.total()).isEqualTo(10L);
        assertThat(result.todo()).isEqualTo(3L);
        assertThat(result.done()).isEqualTo(2L);
        assertThat(result.humanTodo()).isEqualTo(0L);
    }
}
