package TicketCodeIA.domain.model.ticket;

import TicketCodeIA.domain.enums.AgentType;
import TicketCodeIA.domain.enums.Priority;
import TicketCodeIA.domain.enums.TicketStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TicketTest {

    @Test
    void create_setsDefaults() {
        Ticket ticket = Ticket.create("Title", "Desc", Priority.HIGH, 1L, "MyProject");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.TODO);
        assertThat(ticket.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(ticket.getProjectId()).isEqualTo(1L);
        assertThat(ticket.getProjectName()).isEqualTo("MyProject");
        assertThat(ticket.getRetryCount()).isZero();
        assertThat(ticket.getAgentLogs()).isEmpty();
    }

    @Test
    void create_withNullPriority_defaultsToMedium() {
        Ticket ticket = Ticket.create("Title", "Desc", null, null, null);
        assertThat(ticket.getPriority()).isEqualTo(Priority.MEDIUM);
    }

    @Test
    void startDevelopment_setsStatusAndAgent() {
        Ticket ticket = Ticket.create("Title", "Desc", null, null, null);
        ticket.startDevelopment();
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(ticket.getAssignedAgent()).isEqualTo(AgentType.DEVELOPER);
        assertThat(ticket.getAgentLogs()).hasSize(1);
    }

    @Test
    void completeDevelopment_setsCodeReview() {
        Ticket ticket = Ticket.create("Title", "Desc", null, null, null);
        ticket.startDevelopment();
        ticket.completeDevelopment();
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CODE_REVIEW);
    }

    @Test
    void approveReview_setsTestingStatus() {
        Ticket ticket = Ticket.create("Title", "Desc", null, null, null);
        ticket.approveReview();
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.TESTING);
    }

    @Test
    void requestChanges_incrementsRetryCount() {
        Ticket ticket = Ticket.create("Title", "Desc", null, null, null);
        ticket.requestChanges("fix this");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(ticket.getRetryCount()).isEqualTo(1);
    }

    @Test
    void escalate_setsEscalatedStatus() {
        Ticket ticket = Ticket.create("Title", "Desc", null, null, null);
        ticket.escalate("too many retries");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.ESCALATED);
        assertThat(ticket.getAssignedAgent()).isEqualTo(AgentType.HUMAN);
    }

    @Test
    void markDone_setsDoneStatus() {
        Ticket ticket = Ticket.create("Title", "Desc", null, null, null);
        ticket.markDone("all good");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.DONE);
    }

    @Test
    void escalateToHuman_developer_setsHumanDev() {
        Ticket ticket = Ticket.create("Title", "Desc", null, null, null);
        ticket.escalateToHuman(AgentType.DEVELOPER, "dev failed");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.HUMAN_DEV);
        assertThat(ticket.getAssignedAgent()).isEqualTo(AgentType.HUMAN);
    }

    @Test
    void escalateToHuman_reviewer_setsHumanReview() {
        Ticket ticket = Ticket.create("Title", "Desc", null, null, null);
        ticket.escalateToHuman(AgentType.REVIEWER, "review failed");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.HUMAN_REVIEW);
    }

    @Test
    void escalateToHuman_tester_setsHumanTesting() {
        Ticket ticket = Ticket.create("Title", "Desc", null, null, null);
        ticket.escalateToHuman(AgentType.TESTER, "tests failed");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.HUMAN_TESTING);
    }

    @Test
    void escalateToHuman_default_setsHumanTodo() {
        Ticket ticket = Ticket.create("Title", "Desc", null, null, null);
        ticket.escalateToHuman(AgentType.HUMAN, "unknown issue");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.HUMAN_TODO);
    }

    @Test
    void isInFinalState_trueForDoneAndEscalatedAndHumanBoard() {
        Ticket done = Ticket.create("T", "D", null, null, null);
        done.markDone("done");
        assertThat(done.isInFinalState()).isTrue();

        Ticket escalated = Ticket.create("T", "D", null, null, null);
        escalated.escalate("fail");
        assertThat(escalated.isInFinalState()).isTrue();

        Ticket humanDev = Ticket.create("T", "D", null, null, null);
        humanDev.escalateToHuman(AgentType.DEVELOPER, "dev failed");
        assertThat(humanDev.isInFinalState()).isTrue();
    }

    @Test
    void isInFinalState_falseForNonFinal() {
        Ticket ticket = Ticket.create("T", "D", null, null, null);
        assertThat(ticket.isInFinalState()).isFalse();
    }

    @Test
    void hasExceededRetries_returnsTrueWhenExceeded() {
        Ticket ticket = Ticket.create("T", "D", null, null, null);
        ticket.requestChanges("1");
        ticket.requestChanges("2");
        ticket.requestChanges("3");
        assertThat(ticket.hasExceededRetries(3)).isTrue();
    }

    @Test
    void skipToTesting_setsTestingStatus() {
        Ticket ticket = Ticket.create("T", "D", null, null, null);
        ticket.skipToTesting();
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.TESTING);
    }

    @Test
    void completeTests_setsDone() {
        Ticket ticket = Ticket.create("T", "D", null, null, null);
        ticket.completeTests();
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.DONE);
    }

    @Test
    void failTests_incrementsRetryAndSetsInProgress() {
        Ticket ticket = Ticket.create("T", "D", null, null, null);
        ticket.failTests("broken");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(ticket.getRetryCount()).isEqualTo(1);
    }

    @Test
    void addLog_addsToList() {
        Ticket ticket = Ticket.create("T", "D", null, null, null);
        ticket.addLog("first");
        ticket.addLog("second");
        assertThat(ticket.getAgentLogs()).containsExactly("first", "second");
    }
}
