package TicketCodeIA.infrastructure.agent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import TicketCodeIA.application.usecase.agent.CreateTicketsUseCase;
import TicketCodeIA.application.usecase.ticket.ProcessProjectUseCase;

/**
 * Registry that collects all Expert Agent tools and provides them as ToolCallback array.
 * Add new tools here to make them available to the Expert Agent.
 */
@Component
public class ExpertToolRegistry {

    private final ToolCallback[] toolCallbacks;

    public ExpertToolRegistry(CreateTicketsUseCase createTicketsUseCase,
                              @Lazy ProcessProjectUseCase processProjectUseCase,
                              ObjectMapper objectMapper) {
        this.toolCallbacks = new ToolCallback[] {
                new CreateTicketsTool(createTicketsUseCase, objectMapper),
                new StartProjectTool(processProjectUseCase)
        };
    }

    public ToolCallback[] getToolCallbacks() {
        return toolCallbacks;
    }
}
