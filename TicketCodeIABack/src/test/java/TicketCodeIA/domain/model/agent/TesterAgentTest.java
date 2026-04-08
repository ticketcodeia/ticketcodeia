package TicketCodeIA.domain.model.agent;

import TicketCodeIA.domain.enums.AgentType;
import TicketCodeIA.domain.enums.Priority;
import TicketCodeIA.domain.enums.TicketStatus;
import TicketCodeIA.domain.model.ticket.Ticket;
import TicketCodeIA.domain.valueobject.AgentResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TesterAgentTest {

    private final TesterAgent agent = new TesterAgent();

    @Test
    void type_isTester() {
        assertThat(agent.getType()).isEqualTo(AgentType.TESTER);
    }

    @Test
    void canProcess_testingTicket() {
        Ticket ticket = Ticket.create("T", "D", Priority.MEDIUM, null, null);
        ticket.startDevelopment();
        ticket.completeDevelopment();
        ticket.approveReview();
        assertThat(agent.canProcess(ticket)).isTrue();
    }

    @Test
    void canProcess_todoTicket_returnsFalse() {
        Ticket ticket = Ticket.create("T", "D", Priority.MEDIUM, null, null);
        assertThat(agent.canProcess(ticket)).isFalse();
    }

    @Test
    void prepareTicket_assignsTester() {
        Ticket ticket = Ticket.create("T", "D", Priority.MEDIUM, null, null);
        ticket.startDevelopment();
        ticket.completeDevelopment();
        ticket.approveReview();
        agent.prepareTicket(ticket);
        assertThat(ticket.getAssignedAgent()).isEqualTo(AgentType.TESTER);
    }

    @Test
    void applyResult_success_completesTests() {
        Ticket ticket = Ticket.create("T", "D", Priority.MEDIUM, null, null);
        ticket.startDevelopment();
        ticket.completeDevelopment();
        ticket.approveReview();

        boolean shouldContinue = agent.applyResult(ticket, AgentResult.success("passed"), true, true);

        assertThat(shouldContinue).isFalse();
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.DONE);
    }

    @Test
    void applyResult_failure_goesBackToInProgress() {
        Ticket ticket = Ticket.create("T", "D", Priority.MEDIUM, null, null);
        ticket.startDevelopment();
        ticket.completeDevelopment();
        ticket.approveReview();

        boolean shouldContinue = agent.applyResult(ticket, AgentResult.failure("test failed"), true, true);

        assertThat(shouldContinue).isTrue();
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
    }
}
