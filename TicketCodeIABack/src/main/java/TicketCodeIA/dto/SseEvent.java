package TicketCodeIA.dto;

import TicketCodeIA.enums.AgentType;
import TicketCodeIA.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SseEvent {
    private String type;
    private SseEventData data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SseEventData {
        private Long ticketId;
        private TicketStatus status;
        private AgentType agent;
        private String message;
    }

    public static SseEvent ticketUpdated(Long ticketId, TicketStatus status, AgentType agent, String message) {
        return SseEvent.builder()
                .type("TICKET_UPDATED")
                .data(SseEventData.builder()
                        .ticketId(ticketId)
                        .status(status)
                        .agent(agent)
                        .message(message)
                        .build())
                .build();
    }
}
