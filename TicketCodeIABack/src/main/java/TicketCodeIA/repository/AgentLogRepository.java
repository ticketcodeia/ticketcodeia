package TicketCodeIA.repository;

import TicketCodeIA.entity.AgentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentLogRepository extends JpaRepository<AgentLog, Long> {

    List<AgentLog> findByTicketIdOrderByTimestampDesc(Long ticketId);

    List<AgentLog> findTop20ByOrderByTimestampDesc();
}
