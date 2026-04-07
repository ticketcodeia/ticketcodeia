package TicketCodeIA.entity;

import TicketCodeIA.enums.Priority;
import TicketCodeIA.enums.TicketStatus;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class TicketTest {

    @Test
    void builder_setsDefaults() {
        Ticket ticket = Ticket.builder().title("My Ticket").build();

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.TODO);
        assertThat(ticket.getPriority()).isEqualTo(Priority.MEDIUM);
        assertThat(ticket.isEnableCodeReview()).isFalse();
        assertThat(ticket.isEnableTesting()).isFalse();
        assertThat(ticket.getRetryCount()).isZero();
        assertThat(ticket.getAgentLogs()).isNotNull().isEmpty();
        assertThat(ticket.getBranchName()).isNull();
        assertThat(ticket.getAssignedAgent()).isNull();
        assertThat(ticket.getProject()).isNull();
    }

    @Test
    void addAgentLog_appendsToList() {
        Ticket ticket = Ticket.builder().title("T").build();

        ticket.addAgentLog("Step 1 done");
        ticket.addAgentLog("Step 2 done");

        assertThat(ticket.getAgentLogs()).containsExactly("Step 1 done", "Step 2 done");
    }

    @Test
    void addAgentLog_whenLogsIsNull_initializesAndAdds() {
        Ticket ticket = new Ticket();
        ticket.setAgentLogs(null);

        ticket.addAgentLog("First entry");

        assertThat(ticket.getAgentLogs()).containsExactly("First entry");
    }

    @Test
    void addAgentLog_preservesExistingLogs() {
        Ticket ticket = new Ticket();
        ArrayList<String> existing = new ArrayList<>();
        existing.add("existing log");
        ticket.setAgentLogs(existing);

        ticket.addAgentLog("new log");

        assertThat(ticket.getAgentLogs()).containsExactly("existing log", "new log");
    }

    @Test
    void builder_withCustomValues() {
        Ticket ticket = Ticket.builder()
                .title("Feature X")
                .description("Implement feature X")
                .status(TicketStatus.IN_PROGRESS)
                .priority(Priority.HIGH)
                .enableCodeReview(true)
                .enableTesting(true)
                .retryCount(2)
                .build();

        assertThat(ticket.getTitle()).isEqualTo("Feature X");
        assertThat(ticket.getDescription()).isEqualTo("Implement feature X");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(ticket.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(ticket.isEnableCodeReview()).isTrue();
        assertThat(ticket.isEnableTesting()).isTrue();
        assertThat(ticket.getRetryCount()).isEqualTo(2);
    }
}
