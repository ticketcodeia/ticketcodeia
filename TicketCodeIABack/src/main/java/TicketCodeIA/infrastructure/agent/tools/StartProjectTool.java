package TicketCodeIA.infrastructure.agent.tools;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.lang.Nullable;

import TicketCodeIA.application.usecase.ticket.ProcessProjectUseCase;
import lombok.extern.slf4j.Slf4j;

/**
 * Tool callback for starting the project development pipeline.
 * Launches Developer → Reviewer → Tester agents on all project tickets.
 */
@Slf4j
public class StartProjectTool implements ToolCallback {

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

    private final ProcessProjectUseCase processProjectUseCase;

    public StartProjectTool(ProcessProjectUseCase processProjectUseCase) {
        this.processProjectUseCase = processProjectUseCase;
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
        log.info("startProject: Starting pipeline for project {}", projectId);

        processProjectUseCase.executeAsync(projectId);

        return "Project pipeline started! Agents will work on tickets sequentially "
                + "(Developer → Reviewer → Tester). Watch progress on the Agent Board.";
    }
}
