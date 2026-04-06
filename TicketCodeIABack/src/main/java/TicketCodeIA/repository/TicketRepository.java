package TicketCodeIA.repository;

import TicketCodeIA.entity.Ticket;
import TicketCodeIA.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByStatus(TicketStatus status);

    List<Ticket> findByStatusIn(List<TicketStatus> statuses);

    long countByStatus(TicketStatus status);

    List<Ticket> findAllByOrderByCreatedAtDesc();
}
