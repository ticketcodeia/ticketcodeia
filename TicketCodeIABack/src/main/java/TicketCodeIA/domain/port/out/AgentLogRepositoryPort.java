package TicketCodeIA.domain.port.out;

import TicketCodeIA.domain.model.agentlog.AgentLog;

import java.util.List;

public interface AgentLogRepositoryPort {

    AgentLog save(AgentLog agentLog);

    List<AgentLog> findByTicketIdOrderByTimestampDesc(Long ticketId);

    List<AgentLog> findRecentLogs();
}
