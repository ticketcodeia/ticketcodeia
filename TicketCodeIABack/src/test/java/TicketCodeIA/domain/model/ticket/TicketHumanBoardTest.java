package TicketCodeIA.domain.model.ticket;

import TicketCodeIA.domain.enums.AgentType;
import TicketCodeIA.domain.enums.Priority;
import TicketCodeIA.domain.enums.TicketStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketHumanBoardTest {

    private Ticket escalatedTicket() {
        Ticket ticket = Ticket.create("Title", "Desc", Priority.HIGH, null, null);
        ticket.setId(1L);
        ticket.escalate("Max retries");
        return ticket;
    }

    @Test
    void moveToHumanBoard_fromEscalated_setsHumanTodo() {
        Ticket ticket = escalatedTicket();
        ticket.moveToHumanBoard();
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.HUMAN_TODO);
    }

    @Test
    void moveToHumanBoard_fromNonEscalated_throws() {
        Ticket ticket = Ticket.create("T", "D", Priority.MEDIUM, null, null);
        assertThatThrownBy(ticket::moveToHumanBoard)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only escalated");
    }

    @Test
    void startHumanDevelopment_fromHumanTodo_setsHumanDev() {
        Ticket ticket = escalatedTicket();
        ticket.moveToHumanBoard();
        ticket.startHumanDevelopment();
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.HUMAN_DEV);
        assertThat(ticket.getAssignedAgent()).isEqualTo(AgentType.HUMAN);
    }

    @Test
    void startHumanDevelopment_fromWrongStatus_throws() {
        Ticket ticket = escalatedTicket();
        ticket.moveToHumanBoard();
        ticket.startHumanDevelopment();
        assertThatThrownBy(ticket::startHumanDevelopment)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void completeHumanDevelopment_movesToHumanReview() {
        Ticket ticket = escalatedTicket();
        ticket.moveToHumanBoard();
        ticket.startHumanDevelopment();
        ticket.completeHumanDevelopment();
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.HUMAN_REVIEW);
    }

    @Test
    void completeHumanReview_movesToHumanTesting() {
        Ticket ticket = escalatedTicket();
        ticket.moveToHumanBoard();
        ticket.startHumanDevelopment();
        ticket.completeHumanDevelopment();
        ticket.completeHumanReview();
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.HUMAN_TESTING);
    }

    @Test
    void completeHumanTesting_movesToDone() {
        Ticket ticket = escalatedTicket();
        ticket.moveToHumanBoard();
        ticket.startHumanDevelopment();
        ticket.completeHumanDevelopment();
        ticket.completeHumanReview();
        ticket.completeHumanTesting();
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.DONE);
    }

    @Test
    void isOnHumanBoard_forHumanStatuses_returnsTrue() {
        Ticket ticket = escalatedTicket();
        ticket.moveToHumanBoard();
        assertThat(ticket.isOnHumanBoard()).isTrue();

        ticket.startHumanDevelopment();
        assertThat(ticket.isOnHumanBoard()).isTrue();

        ticket.completeHumanDevelopment();
        assertThat(ticket.isOnHumanBoard()).isTrue();

        ticket.completeHumanReview();
        assertThat(ticket.isOnHumanBoard()).isTrue();
    }

    @Test
    void isOnHumanBoard_forNonHumanStatuses_returnsFalse() {
        Ticket ticket = Ticket.create("T", "D", Priority.MEDIUM, null, null);
        assertThat(ticket.isOnHumanBoard()).isFalse();
    }

    @Test
    void fullHumanPipeline_addsLogs() {
        Ticket ticket = escalatedTicket();
        int logsBefore = ticket.getAgentLogs().size();

        ticket.moveToHumanBoard();
        ticket.startHumanDevelopment();
        ticket.completeHumanDevelopment();
        ticket.completeHumanReview();
        ticket.completeHumanTesting();

        assertThat(ticket.getAgentLogs().size()).isEqualTo(logsBefore + 5);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.DONE);
    }
}
