package TicketCodeIA.dto;

import TicketCodeIA.entity.Project;
import TicketCodeIA.entity.Ticket;
import TicketCodeIA.enums.AgentType;
import TicketCodeIA.enums.Priority;
import TicketCodeIA.enums.TicketStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TicketResponseTest {

    @Test
    void fromEntity_mapsAllFieldsCorrectly() {
        Project project = Project.builder()
                .id(10L)
                .name("My Project")
                .description("A test project")
                .createdAt(LocalDateTime.now())
                .build();

        Ticket ticket = Ticket.builder()
                .id(1L)
                .title("Implement login")
                .description("Create login page")
                .status(TicketStatus.IN_PROGRESS)
                .priority(Priority.HIGH)
                .assignedAgent(AgentType.DEVELOPER)
                .agentLogs(List.of("started", "working"))
                .branchName("feature/ticket-1")
                .enableCodeReview(true)
                .enableTesting(false)
                .project(project)
                .build();

        TicketResponse response = TicketResponse.fromEntity(ticket);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Implement login");
        assertThat(response.getDescription()).isEqualTo("Create login page");
        assertThat(response.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(response.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(response.getAssignedAgent()).isEqualTo(AgentType.DEVELOPER);
        assertThat(response.getAgentLogs()).containsExactly("started", "working");
        assertThat(response.getBranchName()).isEqualTo("feature/ticket-1");
        assertThat(response.isEnableCodeReview()).isTrue();
        assertThat(response.isEnableTesting()).isFalse();
        assertThat(response.getProjectId()).isEqualTo(10L);
        assertThat(response.getProjectName()).isEqualTo("My Project");
    }

    @Test
    void fromEntity_withNullProject_setsProjectFieldsToNull() {
        Ticket ticket = Ticket.builder()
                .id(2L)
                .title("No project ticket")
                .description("Desc")
                .build();

        TicketResponse response = TicketResponse.fromEntity(ticket);

        assertThat(response.getProjectId()).isNull();
        assertThat(response.getProjectName()).isNull();
    }

    @Test
    void fromEntity_withDefaultValues_mapsDefaults() {
        Ticket ticket = Ticket.builder()
                .id(3L)
                .title("Simple ticket")
                .build();

        TicketResponse response = TicketResponse.fromEntity(ticket);

        assertThat(response.getStatus()).isEqualTo(TicketStatus.TODO);
        assertThat(response.getPriority()).isEqualTo(Priority.MEDIUM);
        assertThat(response.isEnableCodeReview()).isFalse();
        assertThat(response.isEnableTesting()).isFalse();
        assertThat(response.getAssignedAgent()).isNull();
        assertThat(response.getBranchName()).isNull();
        assertThat(response.getAgentLogs()).isNotNull().isEmpty();
    }
}
