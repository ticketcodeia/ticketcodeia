package TicketCodeIA.domain.model.agent;

import TicketCodeIA.domain.enums.AgentType;
import TicketCodeIA.domain.enums.Priority;
import TicketCodeIA.domain.model.ticket.Ticket;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class POAgentTest {

    private final POAgent agent = new POAgent();

    @Test
    void type_isPO() {
        assertThat(agent.getType()).isEqualTo(AgentType.PO);
    }

    @Test
    void createTicketsFromData_createsTicketsWithCorrectFields() {
        List<Map<String, String>> data = List.of(
                Map.of("title", "Login", "description", "Create login", "priority", "HIGH"),
                Map.of("title", "Dashboard", "description", "Build dashboard", "priority", "MEDIUM")
        );

        List<Ticket> tickets = agent.createTicketsFromData(data, 1L, "MyProject");

        assertThat(tickets).hasSize(2);
        assertThat(tickets.get(0).getTitle()).isEqualTo("Login");
        assertThat(tickets.get(0).getPriority()).isEqualTo(Priority.HIGH);
        assertThat(tickets.get(0).getProjectId()).isEqualTo(1L);
        assertThat(tickets.get(0).getProjectName()).isEqualTo("MyProject");
        assertThat(tickets.get(1).getTitle()).isEqualTo("Dashboard");
        assertThat(tickets.get(1).getPriority()).isEqualTo(Priority.MEDIUM);
    }

    @Test
    void createTicketsFromData_invalidPriority_defaultsToMedium() {
        List<Map<String, String>> data = List.of(
                Map.of("title", "Task", "description", "Desc", "priority", "INVALID")
        );

        List<Ticket> tickets = agent.createTicketsFromData(data, null, null);

        assertThat(tickets.get(0).getPriority()).isEqualTo(Priority.MEDIUM);
    }

    @Test
    void createTicketsFromData_missingFields_usesDefaults() {
        List<Map<String, String>> data = List.of(Map.of());

        List<Ticket> tickets = agent.createTicketsFromData(data, null, null);

        assertThat(tickets).hasSize(1);
        assertThat(tickets.get(0).getTitle()).isEqualTo("Untitled");
        assertThat(tickets.get(0).getPriority()).isEqualTo(Priority.MEDIUM);
    }
}
