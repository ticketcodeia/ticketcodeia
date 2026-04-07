package TicketCodeIA.dto;

import TicketCodeIA.enums.AgentType;
import TicketCodeIA.enums.TicketStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SseEventTest {

    @Test
    void ticketUpdated_buildsEventCorrectly() {
        SseEvent event = SseEvent.ticketUpdated(42L, TicketStatus.IN_PROGRESS, AgentType.DEVELOPER, "Working");

        assertThat(event.getType()).isEqualTo("TICKET_UPDATED");
        assertThat(event.getData()).isNotNull();
        assertThat(event.getData().getTicketId()).isEqualTo(42L);
        assertThat(event.getData().getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(event.getData().getAgent()).isEqualTo(AgentType.DEVELOPER);
        assertThat(event.getData().getMessage()).isEqualTo("Working");
    }

    @Test
    void ticketUpdated_withDoneStatus() {
        SseEvent event = SseEvent.ticketUpdated(1L, TicketStatus.DONE, AgentType.TESTER, "Tests passed");

        assertThat(event.getType()).isEqualTo("TICKET_UPDATED");
        assertThat(event.getData().getStatus()).isEqualTo(TicketStatus.DONE);
        assertThat(event.getData().getAgent()).isEqualTo(AgentType.TESTER);
    }

    @Test
    void ticketUpdated_withEscalatedStatus() {
        SseEvent event = SseEvent.ticketUpdated(5L, TicketStatus.ESCALATED, AgentType.HUMAN, "Needs review");

        assertThat(event.getData().getStatus()).isEqualTo(TicketStatus.ESCALATED);
        assertThat(event.getData().getAgent()).isEqualTo(AgentType.HUMAN);
    }

    @Test
    void builder_allowsManualConstruction() {
        SseEvent.SseEventData data = SseEvent.SseEventData.builder()
                .ticketId(10L)
                .status(TicketStatus.CODE_REVIEW)
                .agent(AgentType.REVIEWER)
                .message("Reviewing code")
                .build();

        SseEvent event = SseEvent.builder().type("CUSTOM").data(data).build();

        assertThat(event.getType()).isEqualTo("CUSTOM");
        assertThat(event.getData().getTicketId()).isEqualTo(10L);
    }
}
