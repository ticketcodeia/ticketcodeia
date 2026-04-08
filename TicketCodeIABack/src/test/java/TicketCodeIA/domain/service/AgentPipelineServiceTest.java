package TicketCodeIA.domain.service;

import TicketCodeIA.domain.enums.Priority;
import TicketCodeIA.domain.enums.TicketStatus;
import TicketCodeIA.domain.model.agent.Agent;
import TicketCodeIA.domain.model.agent.DeveloperAgent;
import TicketCodeIA.domain.model.agent.ReviewerAgent;
import TicketCodeIA.domain.model.agent.TesterAgent;
import TicketCodeIA.domain.model.ticket.Ticket;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AgentPipelineServiceTest {

    private final AgentPipelineService service = new AgentPipelineService();

    @Test
    void resolveNextAgent_todoTicket_returnsDeveloper() {
        Ticket ticket = Ticket.create("T", "D", Priority.MEDIUM, null, null);
        Optional<Agent> agent = service.resolveNextAgent(ticket, true, true);
        assertThat(agent).isPresent();
        assertThat(agent.get()).isInstanceOf(DeveloperAgent.class);
    }

    @Test
    void resolveNextAgent_inProgressTicket_returnsDeveloper() {
        Ticket ticket = Ticket.create("T", "D", Priority.MEDIUM, null, null);
        ticket.startDevelopment();
        Optional<Agent> agent = service.resolveNextAgent(ticket, true, true);
        assertThat(agent).isPresent();
        assertThat(agent.get()).isInstanceOf(DeveloperAgent.class);
    }

    @Test
    void resolveNextAgent_codeReviewTicket_reviewEnabled_returnsReviewer() {
        Ticket ticket = Ticket.create("T", "D", Priority.MEDIUM, null, null);
        ticket.startDevelopment();
        ticket.completeDevelopment();
        Optional<Agent> agent = service.resolveNextAgent(ticket, true, true);
        assertThat(agent).isPresent();
        assertThat(agent.get()).isInstanceOf(ReviewerAgent.class);
    }

    @Test
    void resolveNextAgent_codeReviewTicket_reviewDisabled_testingEnabled_returnsTester() {
        Ticket ticket = Ticket.create("T", "D", Priority.MEDIUM, null, null);
        ticket.startDevelopment();
        ticket.completeDevelopment();
        Optional<Agent> agent = service.resolveNextAgent(ticket, false, true);
        assertThat(agent).isPresent();
        assertThat(agent.get()).isInstanceOf(TesterAgent.class);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.TESTING);
    }

    @Test
    void resolveNextAgent_codeReviewTicket_bothDisabled_marksDone() {
        Ticket ticket = Ticket.create("T", "D", Priority.MEDIUM, null, null);
        ticket.startDevelopment();
        ticket.completeDevelopment();
        Optional<Agent> agent = service.resolveNextAgent(ticket, false, false);
        assertThat(agent).isEmpty();
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.DONE);
    }

    @Test
    void resolveNextAgent_testingTicket_testingEnabled_returnsTester() {
        Ticket ticket = Ticket.create("T", "D", Priority.MEDIUM, null, null);
        ticket.startDevelopment();
        ticket.completeDevelopment();
        ticket.approveReview();
        Optional<Agent> agent = service.resolveNextAgent(ticket, true, true);
        assertThat(agent).isPresent();
        assertThat(agent.get()).isInstanceOf(TesterAgent.class);
    }

    @Test
    void resolveNextAgent_testingTicket_testingDisabled_marksDone() {
        Ticket ticket = Ticket.create("T", "D", Priority.MEDIUM, null, null);
        ticket.startDevelopment();
        ticket.completeDevelopment();
        ticket.approveReview();
        Optional<Agent> agent = service.resolveNextAgent(ticket, true, false);
        assertThat(agent).isEmpty();
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.DONE);
    }

    @Test
    void resolveNextAgent_doneTicket_returnsEmpty() {
        Ticket ticket = Ticket.create("T", "D", Priority.MEDIUM, null, null);
        ticket.startDevelopment();
        ticket.completeDevelopment();
        ticket.approveReview();
        ticket.completeTests();
        Optional<Agent> agent = service.resolveNextAgent(ticket, true, true);
        assertThat(agent).isEmpty();
    }

    @Test
    void shouldEscalate_withinRetries_returnsFalse() {
        Ticket ticket = Ticket.create("T", "D", Priority.MEDIUM, null, null);
        assertThat(service.shouldEscalate(ticket, 3)).isFalse();
    }

    @Test
    void shouldEscalate_exceededRetries_returnsTrue() {
        Ticket ticket = Ticket.create("T", "D", Priority.MEDIUM, null, null);
        ticket.startDevelopment();
        ticket.completeDevelopment();
        ticket.requestChanges("fix 1");
        ticket.completeDevelopment();
        ticket.requestChanges("fix 2");
        ticket.completeDevelopment();
        ticket.requestChanges("fix 3");
        assertThat(service.shouldEscalate(ticket, 3)).isTrue();
    }
}
