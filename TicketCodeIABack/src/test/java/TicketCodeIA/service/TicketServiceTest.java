package TicketCodeIA.service;

import TicketCodeIA.dto.TicketRequest;
import TicketCodeIA.dto.TicketResponse;
import TicketCodeIA.dto.TicketStats;
import TicketCodeIA.entity.Ticket;
import TicketCodeIA.enums.Priority;
import TicketCodeIA.enums.TicketStatus;
import TicketCodeIA.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private TicketService ticketService;

    private Ticket buildTicket(Long id, TicketStatus status) {
        return Ticket.builder()
                .id(id)
                .title("Ticket " + id)
                .description("Description " + id)
                .status(status)
                .priority(Priority.MEDIUM)
                .build();
    }

    @Test
    void createTicket_savesTicketWithDefaults() {
        TicketRequest request = new TicketRequest();
        request.setTitle("New Feature");
        request.setDescription("Implement it");

        Ticket saved = buildTicket(1L, TicketStatus.TODO);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(saved);

        TicketResponse result = ticketService.createTicket(request);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(TicketStatus.TODO);

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("New Feature");
        assertThat(captor.getValue().getStatus()).isEqualTo(TicketStatus.TODO);
        assertThat(captor.getValue().getPriority()).isEqualTo(Priority.MEDIUM);
    }

    @Test
    void createTicket_withExplicitPriority_usesThatPriority() {
        TicketRequest request = new TicketRequest();
        request.setTitle("Critical Bug");
        request.setPriority(Priority.CRITICAL);

        Ticket saved = buildTicket(1L, TicketStatus.TODO);
        saved.setPriority(Priority.CRITICAL);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(saved);

        ticketService.createTicket(request);

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(captor.capture());
        assertThat(captor.getValue().getPriority()).isEqualTo(Priority.CRITICAL);
    }

    @Test
    void getAllTickets_withNoFilter_returnsAll() {
        List<Ticket> tickets = List.of(buildTicket(1L, TicketStatus.TODO), buildTicket(2L, TicketStatus.DONE));
        when(ticketRepository.findAllByOrderByCreatedAtDesc()).thenReturn(tickets);

        List<TicketResponse> result = ticketService.getAllTickets(null, null);

        assertThat(result).hasSize(2);
        verify(ticketRepository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void getAllTickets_withStatusOnly_filtersByStatus() {
        List<Ticket> escalated = List.of(buildTicket(1L, TicketStatus.ESCALATED));
        when(ticketRepository.findByStatus(TicketStatus.ESCALATED)).thenReturn(escalated);

        List<TicketResponse> result = ticketService.getAllTickets(null, TicketStatus.ESCALATED);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(TicketStatus.ESCALATED);
        verify(ticketRepository).findByStatus(TicketStatus.ESCALATED);
    }

    @Test
    void getAllTickets_withProjectIdOnly_filtersByProject() {
        List<Ticket> tickets = List.of(buildTicket(1L, TicketStatus.TODO));
        when(ticketRepository.findByProjectIdOrderByCreatedAtDesc(10L)).thenReturn(tickets);

        List<TicketResponse> result = ticketService.getAllTickets(10L, null);

        assertThat(result).hasSize(1);
        verify(ticketRepository).findByProjectIdOrderByCreatedAtDesc(10L);
    }

    @Test
    void getAllTickets_withProjectIdAndStatus_filtersBoth() {
        List<Ticket> tickets = List.of(buildTicket(1L, TicketStatus.IN_PROGRESS));
        when(ticketRepository.findByProjectIdAndStatus(10L, TicketStatus.IN_PROGRESS)).thenReturn(tickets);

        List<TicketResponse> result = ticketService.getAllTickets(10L, TicketStatus.IN_PROGRESS);

        assertThat(result).hasSize(1);
        verify(ticketRepository).findByProjectIdAndStatus(10L, TicketStatus.IN_PROGRESS);
    }

    @Test
    void getTicketById_whenFound_returnsResponse() {
        Ticket ticket = buildTicket(1L, TicketStatus.TODO);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        TicketResponse result = ticketService.getTicketById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getTicketById_whenNotFound_throwsException() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.getTicketById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Ticket not found");
    }

    @Test
    void updateTicket_updatesProvidedFields() {
        Ticket existing = buildTicket(1L, TicketStatus.TODO);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(ticketRepository.save(any())).thenReturn(existing);

        TicketRequest request = new TicketRequest();
        request.setTitle("Updated Title");
        request.setStatus(TicketStatus.IN_PROGRESS);
        request.setPriority(Priority.HIGH);

        ticketService.updateTicket(1L, request);

        assertThat(existing.getTitle()).isEqualTo("Updated Title");
        assertThat(existing.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(existing.getPriority()).isEqualTo(Priority.HIGH);
    }

    @Test
    void updateTicket_whenNotFound_throwsException() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        TicketRequest request = new TicketRequest();
        assertThatThrownBy(() -> ticketService.updateTicket(99L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Ticket not found");
    }

    @Test
    void getStats_returnsAggregatedCounts() {
        when(ticketRepository.count()).thenReturn(10L);
        when(ticketRepository.countByStatus(TicketStatus.TODO)).thenReturn(3L);
        when(ticketRepository.countByStatus(TicketStatus.IN_PROGRESS)).thenReturn(2L);
        when(ticketRepository.countByStatus(TicketStatus.CODE_REVIEW)).thenReturn(1L);
        when(ticketRepository.countByStatus(TicketStatus.TESTING)).thenReturn(1L);
        when(ticketRepository.countByStatus(TicketStatus.DONE)).thenReturn(2L);
        when(ticketRepository.countByStatus(TicketStatus.ESCALATED)).thenReturn(1L);

        TicketStats stats = ticketService.getStats();

        assertThat(stats.getTotal()).isEqualTo(10);
        assertThat(stats.getTodo()).isEqualTo(3);
        assertThat(stats.getInProgress()).isEqualTo(2);
        assertThat(stats.getCodeReview()).isEqualTo(1);
        assertThat(stats.getTesting()).isEqualTo(1);
        assertThat(stats.getDone()).isEqualTo(2);
        assertThat(stats.getEscalated()).isEqualTo(1);
    }

    @Test
    void saveAgentFlags_updatesFlags() {
        Ticket ticket = buildTicket(1L, TicketStatus.TODO);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenReturn(ticket);

        ticketService.saveAgentFlags(1L, true, true);

        assertThat(ticket.isEnableCodeReview()).isTrue();
        assertThat(ticket.isEnableTesting()).isTrue();
        verify(ticketRepository).save(ticket);
    }

    @Test
    void saveAgentFlags_whenTicketNotFound_throwsException() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.saveAgentFlags(99L, true, false))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Ticket not found");
    }

    @Test
    void getTicketEntity_returnsRawEntity() {
        Ticket ticket = buildTicket(1L, TicketStatus.TODO);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        Ticket result = ticketService.getTicketEntity(1L);

        assertThat(result).isEqualTo(ticket);
    }

    @Test
    void saveTicket_delegatesToRepository() {
        Ticket ticket = buildTicket(1L, TicketStatus.IN_PROGRESS);
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        Ticket result = ticketService.saveTicket(ticket);

        assertThat(result).isEqualTo(ticket);
        verify(ticketRepository).save(ticket);
    }
}
