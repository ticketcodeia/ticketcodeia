package TicketCodeIA.infrastructure.agent.tools;

import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.lang.Nullable;

import TicketCodeIA.application.query.TicketResult;
import TicketCodeIA.domain.model.ticket.Ticket;
import TicketCodeIA.domain.port.out.TicketRepositoryPort;
import lombok.extern.slf4j.Slf4j;

/**
 * Tool callback that lets the Expert Agent list tickets for the current project.
 * Useful for the Expert to see ticket statuses before changing them or assigning them.
 */
@Slf4j
public class ListTicketsTool implements ToolCallback {

    private static final String INPUT_SCHEMA = """
            {
              "type": "object",
              "properties": {},
              "required": []
            }
            """;

    private final ToolDefinition toolDefinition = DefaultToolDefinition.builder()
            .name("listTickets")
            .description("List all tickets for the current project with their ID, title, status, priority, and assigned agent. "
                    + "Use this to see the current state of all tickets before changing status or assigning them.")
            .inputSchema(INPUT_SCHEMA)
            .build();

    private final TicketRepositoryPort ticketRepository;

    public ListTicketsTool(TicketRepositoryPort ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return toolDefinition;
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return ToolMetadata.builder().build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable org.springframework.ai.chat.model.ToolContext toolContext) {
        return execute();
    }

    private String execute() {
        Long projectId = ExpertToolContext.getCurrentProjectId();
        if (projectId == null) {
            return "Error: No project selected.";
        }

        List<Ticket> tickets = ticketRepository.findByProjectIdOrderByCreatedAtDesc(projectId);

        if (tickets.isEmpty()) {
            return "No tickets found for this project.";
        }

        String summary = tickets.stream()
                .map(t -> "- #" + t.getId()
                        + " [" + t.getStatus() + "]"
                        + " [" + t.getPriority() + "]"
                        + " assigned=" + (t.getAssignedAgent() != null ? t.getAssignedAgent() : "none")
                        + " | " + t.getTitle())
                .collect(Collectors.joining("\n"));

        return "Project tickets (" + tickets.size() + "):\n" + summary;
    }
}
