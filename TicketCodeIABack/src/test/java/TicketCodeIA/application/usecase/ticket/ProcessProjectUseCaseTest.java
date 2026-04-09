package TicketCodeIA.application.usecase.ticket;

import TicketCodeIA.domain.enums.Priority;
import TicketCodeIA.domain.enums.TicketStatus;
import TicketCodeIA.domain.model.ticket.Ticket;
import TicketCodeIA.domain.port.in.POAgentPort;
import TicketCodeIA.domain.port.out.TicketRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessProjectUseCaseTest {

    @Mock private TicketRepositoryPort ticketRepository;
    @Mock private ProcessTicketUseCase processTicketUseCase;
    @Mock private POAgentPort poAgentPort;
    @InjectMocks private ProcessProjectUseCase useCase;

    private Ticket todoTicket(Long id, boolean enableCodeReview, boolean enableTesting) {
        Ticket t = Ticket.create("Title " + id, "Desc", Priority.MEDIUM, 1L, "Project");
        t.setId(id);
        t.setEnableCodeReview(enableCodeReview);
        t.setEnableTesting(enableTesting);
        return t;
    }

    @Test
    void execute_noTodoTickets_doesNothing() {
        when(ticketRepository.findByProjectIdAndStatus(1L, TicketStatus.TODO))
                .thenReturn(List.of());

        useCase.execute(1L);

        verify(processTicketUseCase, never()).execute(anyLong(), anyBoolean(), anyBoolean());
        verify(poAgentPort, never()).chooseNextTicket(anyList(), anyList());
    }

    @Test
    void execute_poAgentChoosesNextTicket() {
        Ticket t1 = todoTicket(1L, true, true);
        Ticket t2 = todoTicket(2L, false, false);

        // Initial load
        when(ticketRepository.findByProjectIdAndStatus(1L, TicketStatus.TODO))
                .thenReturn(List.of(t1, t2))  // first call (initial)
                .thenReturn(List.of(t1, t2))  // second call (iteration 1 reload)
                .thenReturn(List.of(t1));      // third call (iteration 2 reload, t2 done)

        when(ticketRepository.countByProjectIdAndStatusIn(eq(1L), anyList()))
                .thenReturn(0L);
        when(ticketRepository.findByProjectId(1L))
                .thenReturn(List.of(t1, t2));

        // PO chooses t2 first, then t1
        when(poAgentPort.chooseNextTicket(anyList(), anyList()))
                .thenReturn(2L)
                .thenReturn(1L);

        when(ticketRepository.findById(2L)).thenReturn(Optional.of(t2));
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(t1));

        useCase.execute(1L);

        verify(poAgentPort, times(2)).chooseNextTicket(anyList(), anyList());
        verify(processTicketUseCase).execute(2L, false, false);
        verify(processTicketUseCase).execute(1L, true, true);
    }

    @Test
    void execute_skipsTicketNoLongerTodo() {
        Ticket t1 = todoTicket(1L, true, true);
        Ticket t1Done = todoTicket(1L, true, true);
        t1Done.markDone("already done");

        when(ticketRepository.findByProjectIdAndStatus(1L, TicketStatus.TODO))
                .thenReturn(List.of(t1))   // initial
                .thenReturn(List.of(t1));  // reload

        when(ticketRepository.countByProjectIdAndStatusIn(eq(1L), anyList()))
                .thenReturn(0L);
        when(ticketRepository.findByProjectId(1L))
                .thenReturn(List.of(t1Done));
        when(poAgentPort.chooseNextTicket(anyList(), anyList()))
                .thenReturn(1L);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(t1Done));

        useCase.execute(1L);

        verify(processTicketUseCase, never()).execute(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void execute_respectsPerTicketFlags() {
        Ticket t1 = todoTicket(1L, false, true);
        when(ticketRepository.findByProjectIdAndStatus(1L, TicketStatus.TODO))
                .thenReturn(List.of(t1))
                .thenReturn(List.of(t1));
        when(ticketRepository.countByProjectIdAndStatusIn(eq(1L), anyList()))
                .thenReturn(0L);
        when(ticketRepository.findByProjectId(1L))
                .thenReturn(List.of(t1));
        when(poAgentPort.chooseNextTicket(anyList(), anyList()))
                .thenReturn(1L);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(t1));

        useCase.execute(1L);

        verify(processTicketUseCase).execute(1L, false, true);
    }
}
