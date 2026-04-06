package TicketCodeIA.service;

import TicketCodeIA.dto.TicketRequest;
import TicketCodeIA.dto.TicketResponse;
import TicketCodeIA.dto.TicketStats;
import TicketCodeIA.entity.Ticket;
import TicketCodeIA.enums.Priority;
import TicketCodeIA.enums.TicketStatus;
import TicketCodeIA.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;

    @Transactional
    public TicketResponse createTicket(TicketRequest request) {
        Ticket ticket = Ticket.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority() != null ? request.getPriority() : Priority.MEDIUM)
                .status(TicketStatus.TODO)
                .build();

        Ticket saved = ticketRepository.save(ticket);
        return TicketResponse.fromEntity(saved);
    }

    public List<TicketResponse> getAllTickets(TicketStatus status) {
        List<Ticket> tickets;
        if (status != null) {
            tickets = ticketRepository.findByStatus(status);
        } else {
            tickets = ticketRepository.findAllByOrderByCreatedAtDesc();
        }
        return tickets.stream()
                .map(TicketResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public TicketResponse getTicketById(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found with id: " + id));
        return TicketResponse.fromEntity(ticket);
    }

    @Transactional
    public TicketResponse updateTicket(Long id, TicketRequest request) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found with id: " + id));

        if (request.getTitle() != null) {
            ticket.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            ticket.setDescription(request.getDescription());
        }
        if (request.getPriority() != null) {
            ticket.setPriority(request.getPriority());
        }
        if (request.getStatus() != null) {
            ticket.setStatus(request.getStatus());
        }

        Ticket updated = ticketRepository.save(ticket);
        return TicketResponse.fromEntity(updated);
    }

    public TicketStats getStats() {
        return TicketStats.builder()
                .total(ticketRepository.count())
                .todo(ticketRepository.countByStatus(TicketStatus.TODO))
                .inProgress(ticketRepository.countByStatus(TicketStatus.IN_PROGRESS))
                .codeReview(ticketRepository.countByStatus(TicketStatus.CODE_REVIEW))
                .testing(ticketRepository.countByStatus(TicketStatus.TESTING))
                .done(ticketRepository.countByStatus(TicketStatus.DONE))
                .escalated(ticketRepository.countByStatus(TicketStatus.ESCALATED))
                .build();
    }

    public Ticket getTicketEntity(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found with id: " + id));
    }

    @Transactional
    public Ticket saveTicket(Ticket ticket) {
        return ticketRepository.save(ticket);
    }
}
