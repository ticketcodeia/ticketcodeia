package TicketCodeIA.infrastructure.agent.tools;

import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.lang.Nullable;

import TicketCodeIA.application.command.CreateTicketData;
import TicketCodeIA.application.query.TicketResult;
import TicketCodeIA.application.usecase.agent.CreateTicketsUseCase;
import lombok.extern.slf4j.Slf4j;

/**
 * Tool callback for creating development tickets in the database.
 * Called by the Expert Agent when the user confirms ticket creation.
 */
@Slf4j
public class CreateTicketsTool implements ToolCallback {

    private static final String INPUT_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "ticketsJson": {
                  "type": "string",
                  "description": "A JSON array string of ticket objects. Each ticket must have: title (string), description (string), priority (LOW/MEDIUM/HIGH/CRITICAL), enableCodeReview (boolean), enableTesting (boolean). Example: [{\\"title\\":\\"Auth module\\",\\"description\\":\\"Build login and registration\\",\\"priority\\":\\"HIGH\\",\\"enableCodeReview\\":false,\\"enableTesting\\":false}]"
                }
              },
              "required": ["ticketsJson"]
            }
            """;

    private final ToolDefinition toolDefinition = DefaultToolDefinition.builder()
            .name("createTickets")
            .description("Create development tickets in the database. "
                    + "You MUST pass ticketsJson as a JSON array STRING of ticket objects. "
                    + "Each ticket: {title, description, priority (LOW/MEDIUM/HIGH/CRITICAL), enableCodeReview (bool), enableTesting (bool)}. "
                    + "Call ONLY after user confirms.")
            .inputSchema(INPUT_SCHEMA)
            .build();

    private final CreateTicketsUseCase createTicketsUseCase;
    private final ObjectMapper objectMapper;

    public CreateTicketsTool(CreateTicketsUseCase createTicketsUseCase, ObjectMapper objectMapper) {
        this.createTicketsUseCase = createTicketsUseCase;
        this.objectMapper = objectMapper;
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
        log.info("createTickets raw input: {}", toolInput);
        return execute(toolInput);
    }

    private String execute(String rawJson) {
        Long projectId = ExpertToolContext.getCurrentProjectId();

        try {
            var root = objectMapper.readTree(rawJson);
            String ticketsJsonStr;

            if (root.has("ticketsJson")) {
                ticketsJsonStr = root.get("ticketsJson").asText();
            } else if (root.isArray()) {
                ticketsJsonStr = rawJson;
            } else if (root.has("tickets")) {
                ticketsJsonStr = root.get("tickets").toString();
            } else {
                log.error("createTickets: Unexpected JSON: {}", rawJson);
                return "Error: Please call createTickets with ticketsJson parameter containing a JSON array of tickets.";
            }

            log.info("createTickets: ticketsJson value = {}", ticketsJsonStr);

            List<CreateTicketData> tickets = objectMapper.readValue(
                    ticketsJsonStr, new TypeReference<List<CreateTicketData>>() {});

            log.info("createTickets: Parsed {} tickets for project {}", tickets.size(), projectId);

            if (tickets.isEmpty()) {
                return "Error: Empty tickets array.";
            }

            List<TicketResult> results = createTicketsUseCase.execute(tickets, projectId);

            String ticketSummary = results.stream()
                    .map(t -> "- #" + t.id() + " [" + t.priority() + "] " + t.title())
                    .collect(Collectors.joining("\n"));

            return "Successfully created " + results.size() + " tickets:\n" + ticketSummary;

        } catch (Exception e) {
            log.error("createTickets: Error parsing: {}", rawJson, e);
            return "Error parsing tickets: " + e.getMessage();
        }
    }
}
