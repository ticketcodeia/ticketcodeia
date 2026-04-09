package TicketCodeIA.domain.port.out;

import TicketCodeIA.domain.enums.TicketStatus;
import TicketCodeIA.domain.model.ticket.Ticket;

import java.util.List;
import java.util.Optional;

public interface TicketRepositoryPort {

    Ticket save(Ticket ticket);

    Optional<Ticket> findById(Long id);

    Optional<Ticket> findByIdWithProject(Long id);

    List<Ticket> findAll();

    List<Ticket> findByStatus(TicketStatus status);

    List<Ticket> findByProjectId(Long projectId);

    List<Ticket> findByProjectIdAndStatus(Long projectId, TicketStatus status);

    List<Ticket> findAllOrderByCreatedAtDesc();

    List<Ticket> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    long count();

    long countByStatus(TicketStatus status);

    long countByProjectIdAndStatusIn(Long projectId, List<TicketStatus> statuses);
}
