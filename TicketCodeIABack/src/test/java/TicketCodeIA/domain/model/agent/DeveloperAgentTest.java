package TicketCodeIA.domain.model.agent;

import TicketCodeIA.domain.enums.AgentType;
import TicketCodeIA.domain.enums.Priority;
import TicketCodeIA.domain.enums.TicketStatus;
import TicketCodeIA.domain.model.ticket.Ticket;
import TicketCodeIA.domain.valueobject.AgentResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeveloperAgentTest {

    private final DeveloperAgent agent = new DeveloperAgent();

    @Test
    void type_isDeveloper() {
        assertThat(agent.getType()).isEqualTo(AgentType.DEVELOPER);
    }

    @Test
    void canProcess_todoTicket() {
        Ticket ticket = Ticket.create("T", "D", Priority.MEDIUM, null, null);
        assertThat(agent.canProcess(ticket)).isTrue();
    }

    @Test
    void canProcess_inProgressTicket() {
        Ticket ticket = Ticket.create("T", "D", Priority.MEDIUM, null, null);
        ticket.startDevelopment();
        assertThat(agent.canProcess(ticket)).isTrue();
    }

    @Test
    void canProcess_codeReviewTicket_returnsFalse() {
        Ticket ticket = Ticket.create("T", "D", Priority.MEDIUM, null, null);
        ticket.startDevelopment();
        ticket.completeDevelopment();
        assertThat(agent.canProcess(ticket)).isFalse();
    }

    @Test
    void prepareTicket_setsInProgressAndAssignsDeveloper() {
        Ticket ticket = Ticket.create("T", "D", Priority.MEDIUM, null, null);
        agent.prepareTicket(ticket);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(ticket.getAssignedAgent()).isEqualTo(AgentType.DEVELOPER);
    }

    @Test
    void applyResult_failure_escalates() {
        Ticket ticket = Ticket.create("T", "D", Priority.MEDIUM, null, null);
        ticket.setId(1L);
        ticket.startDevelopment();

        boolean shouldContinue = agent.applyResult(ticket, AgentResult.failure("error"), true, true);

        assertThat(shouldContinue).isFalse();
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.HUMAN_DEV);
    }

    @Test
    void applyResult_success_bothDisabled_marksDone() {
        Ticket ticket = Ticket.create("T", "D", Priority.MEDIUM, null, null);
        ticket.setId(1L);
        ticket.startDevelopment();

        boolean shouldContinue = agent.applyResult(ticket, AgentResult.success("done"), false, false);

        assertThat(shouldContinue).isFalse();
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.DONE);
    }

    @Test
    void applyResult_success_reviewEnabled_movesToCodeReview() {
        Ticket ticket = Ticket.create("T", "D", Priority.MEDIUM, null, null);
        ticket.setId(1L);
        ticket.startDevelopment();

        boolean shouldContinue = agent.applyResult(ticket, AgentResult.success("done"), true, true);

        assertThat(shouldContinue).isTrue();
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CODE_REVIEW);
        assertThat(ticket.getBranchName()).isEqualTo("feature/ticket-1");
    }

    @Test
    void applyResult_success_reviewDisabled_skipsToTesting() {
        Ticket ticket = Ticket.create("T", "D", Priority.MEDIUM, null, null);
        ticket.setId(1L);
        ticket.startDevelopment();

        boolean shouldContinue = agent.applyResult(ticket, AgentResult.success("done"), false, true);

        assertThat(shouldContinue).isTrue();
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.TESTING);
    }
}
