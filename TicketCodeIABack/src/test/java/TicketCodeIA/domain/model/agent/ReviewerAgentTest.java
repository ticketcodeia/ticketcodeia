package TicketCodeIA.domain.model.agent;

import TicketCodeIA.domain.enums.AgentType;
import TicketCodeIA.domain.enums.Priority;
import TicketCodeIA.domain.enums.TicketStatus;
import TicketCodeIA.domain.model.ticket.Ticket;
import TicketCodeIA.domain.valueobject.AgentResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewerAgentTest {

    private final ReviewerAgent agent = new ReviewerAgent();

    @Test
    void type_isReviewer() {
        assertThat(agent.getType()).isEqualTo(AgentType.REVIEWER);
    }

    @Test
    void canProcess_codeReviewTicket() {
        Ticket ticket = Ticket.create("T", "D", Priority.MEDIUM, null, null);
        ticket.startDevelopment();
        ticket.completeDevelopment();
        assertThat(agent.canProcess(ticket)).isTrue();
    }

    @Test
    void canProcess_todoTicket_returnsFalse() {
        Ticket ticket = Ticket.create("T", "D", Priority.MEDIUM, null, null);
        assertThat(agent.canProcess(ticket)).isFalse();
    }

    @Test
    void prepareTicket_assignsReviewer() {
        Ticket ticket = Ticket.create("T", "D", Priority.MEDIUM, null, null);
        ticket.startDevelopment();
        ticket.completeDevelopment();
        agent.prepareTicket(ticket);
        assertThat(ticket.getAssignedAgent()).isEqualTo(AgentType.REVIEWER);
    }

    @Test
    void applyResult_failure_escalates() {
        Ticket ticket = Ticket.create("T", "D", Priority.MEDIUM, null, null);
        ticket.startDevelopment();
        ticket.completeDevelopment();

        boolean shouldContinue = agent.applyResult(ticket, AgentResult.failure("error"), true, true);

        assertThat(shouldContinue).isFalse();
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.HUMAN_REVIEW);
    }

    @Test
    void applyResult_needsChanges_returnsToInProgress() {
        Ticket ticket = Ticket.create("T", "D", Priority.MEDIUM, null, null);
        ticket.startDevelopment();
        ticket.completeDevelopment();

        boolean shouldContinue = agent.applyResult(ticket, AgentResult.needsChanges("fix this"), true, true);

        assertThat(shouldContinue).isTrue();
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
    }

    @Test
    void applyResult_approved_testingDisabled_marksDone() {
        Ticket ticket = Ticket.create("T", "D", Priority.MEDIUM, null, null);
        ticket.startDevelopment();
        ticket.completeDevelopment();

        boolean shouldContinue = agent.applyResult(ticket, AgentResult.success("approved"), true, false);

        assertThat(shouldContinue).isFalse();
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.DONE);
    }

    @Test
    void applyResult_approved_testingEnabled_movesToTesting() {
        Ticket ticket = Ticket.create("T", "D", Priority.MEDIUM, null, null);
        ticket.startDevelopment();
        ticket.completeDevelopment();

        boolean shouldContinue = agent.applyResult(ticket, AgentResult.success("approved"), true, true);

        assertThat(shouldContinue).isTrue();
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.TESTING);
    }
}
