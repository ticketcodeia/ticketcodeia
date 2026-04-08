package TicketCodeIA.application.usecase.ticket;

import TicketCodeIA.application.query.TicketQuery;
import TicketCodeIA.application.query.TicketResult;
import TicketCodeIA.domain.enums.Priority;
import TicketCodeIA.domain.enums.TicketStatus;
import TicketCodeIA.domain.exception.TicketNotFoundException;
import TicketCodeIA.domain.model.ticket.Ticket;
import TicketCodeIA.domain.port.out.TicketRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTicketUseCaseTest {

    @Mock private TicketRepositoryPort ticketRepository;
    @InjectMocks private GetTicketUseCase useCase;

    private Ticket sampleTicket(Long id) {
        Ticket t = Ticket.create("Title " + id, "Desc", Priority.MEDIUM, null, null);
        t.setId(id);
        return t;
    }

    @Test
    void getById_returnsTicket() {
        when(ticketRepository.findByIdWithProject(1L)).thenReturn(Optional.of(sampleTicket(1L)));
        TicketResult result = useCase.getById(1L);
        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    void getById_notFound_throws() {
        when(ticketRepository.findByIdWithProject(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.getById(99L)).isInstanceOf(TicketNotFoundException.class);
    }

    @Test
    void getAll_noFilters_returnsAll() {
        when(ticketRepository.findAllOrderByCreatedAtDesc())
                .thenReturn(List.of(sampleTicket(1L), sampleTicket(2L)));
        List<TicketResult> results = useCase.getAll(new TicketQuery(null, null));
        assertThat(results).hasSize(2);
    }

    @Test
    void getAll_byStatus_filtersCorrectly() {
        when(ticketRepository.findByStatus(TicketStatus.TODO))
                .thenReturn(List.of(sampleTicket(1L)));
        List<TicketResult> results = useCase.getAll(new TicketQuery(null, TicketStatus.TODO));
        assertThat(results).hasSize(1);
    }

    @Test
    void getAll_byProjectId_filtersCorrectly() {
        when(ticketRepository.findByProjectIdOrderByCreatedAtDesc(5L))
                .thenReturn(List.of(sampleTicket(1L)));
        List<TicketResult> results = useCase.getAll(new TicketQuery(5L, null));
        assertThat(results).hasSize(1);
    }

    @Test
    void getAll_byProjectIdAndStatus_filtersCorrectly() {
        when(ticketRepository.findByProjectIdAndStatus(5L, TicketStatus.DONE))
                .thenReturn(List.of());
        List<TicketResult> results = useCase.getAll(new TicketQuery(5L, TicketStatus.DONE));
        assertThat(results).isEmpty();
    }
}
