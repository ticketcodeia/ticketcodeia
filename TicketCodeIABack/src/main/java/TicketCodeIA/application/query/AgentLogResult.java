package TicketCodeIA.application.query;

import TicketCodeIA.domain.enums.AgentType;
import TicketCodeIA.domain.model.agentlog.AgentLog;

import java.time.LocalDateTime;

public record AgentLogResult(
        Long id,
        Long ticketId,
        AgentType agentType,
        String action,
        String message,
        LocalDateTime timestamp
) {
    public static AgentLogResult fromDomain(AgentLog log) {
        return new AgentLogResult(
                log.getId(),
                log.getTicketId(),
                log.getAgentType(),
                log.getAction(),
                log.getMessage(),
                log.getTimestamp()
        );
    }
}
