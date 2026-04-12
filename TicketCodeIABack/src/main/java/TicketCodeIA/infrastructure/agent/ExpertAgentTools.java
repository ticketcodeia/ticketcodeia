package TicketCodeIA.infrastructure.agent;

import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import TicketCodeIA.application.query.TicketResult;
import TicketCodeIA.application.usecase.agent.CreateTicketsUseCase;
import TicketCodeIA.application.usecase.ticket.ProcessProjectUseCase;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ExpertAgentTools {

    private final CreateTicketsUseCase createTicketsUseCase;
    private final ProcessProjectUseCase processProjectUseCase;
    private final ObjectMapper objectMapper;

    public ExpertAgentTools(CreateTicketsUseCase createTicketsUseCase,
                            @Lazy ProcessProjectUseCase processProjectUseCase,
                            ObjectMapper objectMapper) {
        this.createTicketsUseCase = createTicketsUseCase;
        this.processProjectUseCase = processProjectUseCase;
        this.objectMapper = objectMapper;
    }

    private static final ThreadLocal<Long> CURRENT_PROJECT_ID = new ThreadLocal<>();

    public static void setCurrentProjectId(Long projectId) {
        CURRENT_PROJECT_ID.set(projectId);
    }

    public static void clearCurrentProjectId() {
        CURRENT_PROJECT_ID.remove();
    }

    public ToolCallback[] getToolCallbacks() {
        return new ToolCallback[] { new CreateTicketsToolCallback(), new StartProjectToolCallback() };
    }

    // ── Custom ToolCallback implementations ─────────────────────────────────

    private class CreateTicketsToolCallback implements ToolCallback {

        // Simple flat schema: one string property containing the JSON array
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
                        + "You MUST pass ticketsJson as a JSON array string of ticket objects. "
                        + "Each ticket: {title, description, priority (LOW/MEDIUM/HIGH/CRITICAL), enableCodeReview (bool), enableTesting (bool)}. "
                        + "Call ONLY after user confirms.")
                .inputSchema(INPUT_SCHEMA)
                .build();

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
            return doCreateTickets(toolInput);
        }
    }

    private class StartProjectToolCallback implements ToolCallback {

        private static final String INPUT_SCHEMA = """
                {
                  "type": "object",
                  "properties": {},
                  "required": []
                }
                """;

        private final ToolDefinition toolDefinition = DefaultToolDefinition.builder()
                .name("startProject")
                .description("Start processing a project. Launches the development pipeline: "
                        + "Expert Agent picks tickets, then Developer, Reviewer and Tester agents work sequentially. "
                        + "Call when user asks to start the project.")
                .inputSchema(INPUT_SCHEMA)
                .build();

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
            return doStartProject();
        }
    }

    // ── Tool logic ──────────────────────────────────────────────────────────

    private String doCreateTickets(String rawJson) {
        Long projectId = CURRENT_PROJECT_ID.get();

        try {
            // rawJson is the serialized tool input object: {"ticketsJson": "[...]"}
            // Extract the ticketsJson string value
            var root = objectMapper.readTree(rawJson);
            String ticketsJsonStr;

            if (root.has("ticketsJson")) {
                ticketsJsonStr = root.get("ticketsJson").asText();
            } else if (root.isArray()) {
                // Claude sent the array directly
                ticketsJsonStr = rawJson;
            } else {
                // Maybe Claude sent the tickets inline as an object with a "tickets" key
                if (root.has("tickets")) {
                    ticketsJsonStr = root.get("tickets").toString();
                } else {
                    log.error("createTickets: Unexpected JSON: {}", rawJson);
                    return "Error: Please call createTickets with ticketsJson parameter containing a JSON array of tickets.";
                }
            }

            log.info("createTickets: ticketsJson value = {}", ticketsJsonStr);

            List<TicketData> tickets = objectMapper.readValue(
                    ticketsJsonStr, new TypeReference<List<TicketData>>() {});

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

    private String doStartProject() {
        Long projectId = CURRENT_PROJECT_ID.get();
        if (projectId == null) {
            return "Error: No project selected.";
        }
        log.info("startProject: Starting pipeline for project {}", projectId);

        processProjectUseCase.executeAsync(projectId);

        return "Project pipeline started! Agents will work on tickets sequentially "
                + "(Developer → Reviewer → Tester). Watch progress on the Agent Board.";
    }

    /** A single ticket to create. */
    public record TicketData(
            @JsonProperty("title") String title,
            @JsonProperty("description") String description,
            @JsonProperty("priority") String priority,
            @JsonProperty("enableCodeReview") boolean enableCodeReview,
            @JsonProperty("enableTesting") boolean enableTesting) {}
}
