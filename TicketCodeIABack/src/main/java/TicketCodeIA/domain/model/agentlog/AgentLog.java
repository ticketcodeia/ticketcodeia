package TicketCodeIA.domain.model.agentlog;

import TicketCodeIA.domain.enums.AgentType;

import java.time.LocalDateTime;

public class AgentLog {

    private Long id;
    private final Long ticketId;
    private final AgentType agentType;
    private final String action;
    private final String message;
    private LocalDateTime timestamp;

    public AgentLog(Long id, Long ticketId, AgentType agentType, String action, String message, LocalDateTime timestamp) {
        this.id = id;
        this.ticketId = ticketId;
        this.agentType = agentType;
        this.action = action;
        this.message = message;
        this.timestamp = timestamp;
    }

    public static AgentLog create(Long ticketId, AgentType agentType, String action, String message) {
        return new AgentLog(null, ticketId, agentType, action, message, null);
    }

    public Long getId() { return id; }
    public Long getTicketId() { return ticketId; }
    public AgentType getAgentType() { return agentType; }
    public String getAction() { return action; }
    public String getMessage() { return message; }
    public LocalDateTime getTimestamp() { return timestamp; }

    public void setId(Long id) { this.id = id; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
