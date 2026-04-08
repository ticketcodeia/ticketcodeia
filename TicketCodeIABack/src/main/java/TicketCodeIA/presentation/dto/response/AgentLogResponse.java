package TicketCodeIA.presentation.dto.response;

import TicketCodeIA.application.query.AgentLogResult;
import TicketCodeIA.domain.enums.AgentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentLogResponse {
    private Long id;
    private Long ticketId;
    private AgentType agentType;
    private String action;
    private String message;
    private LocalDateTime timestamp;

    public static AgentLogResponse fromResult(AgentLogResult result) {
        return AgentLogResponse.builder()
                .id(result.id())
                .ticketId(result.ticketId())
                .agentType(result.agentType())
                .action(result.action())
                .message(result.message())
                .timestamp(result.timestamp())
                .build();
    }
}
