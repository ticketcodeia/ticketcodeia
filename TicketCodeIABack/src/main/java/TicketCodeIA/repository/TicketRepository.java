package TicketCodeIA.repository;

import TicketCodeIA.entity.Ticket;
import TicketCodeIA.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByStatus(TicketStatus status);

    List<Ticket> findByProjectId(Long projectId);

    List<Ticket> findByProjectIdAndStatus(Long projectId, TicketStatus status);

    long countByStatus(TicketStatus status);

    long countByProjectIdAndStatus(Long projectId, TicketStatus status);

    List<Ticket> findAllByOrderByCreatedAtDesc();

    List<Ticket> findByProjectIdOrderByCreatedAtDesc(Long projectId);
}
