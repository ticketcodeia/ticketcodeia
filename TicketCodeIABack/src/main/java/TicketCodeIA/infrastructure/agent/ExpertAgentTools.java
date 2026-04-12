package TicketCodeIA.infrastructure.agent;

import TicketCodeIA.application.query.TicketResult;
import TicketCodeIA.application.usecase.agent.GenerateTicketsFromRequirementsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpertAgentTools {

    private final GenerateTicketsFromRequirementsUseCase generateTicketsUseCase;

    private static final ThreadLocal<Long> CURRENT_PROJECT_ID = new ThreadLocal<>();

    public static void setCurrentProjectId(Long projectId) {
        CURRENT_PROJECT_ID.set(projectId);
    }

    public static void clearCurrentProjectId() {
        CURRENT_PROJECT_ID.remove();
    }

    @Tool(description = "Create development tickets by sending detailed requirements to the PO Agent. "
            + "Call this when you have gathered enough information about the project and the user confirms. "
            + "The requirements must be detailed and well-structured with features, technical details, and acceptance criteria.")
    public String createTickets(
            @ToolParam(description = "Detailed and structured requirements describing all features to build") String requirements) {
        Long projectId = CURRENT_PROJECT_ID.get();
        log.info("Expert Agent Tool: Creating tickets via PO Agent for project {}", projectId);

        List<TicketResult> tickets = generateTicketsUseCase.execute(requirements, projectId);

        String ticketSummary = tickets.stream()
                .map(t -> "- #" + t.id() + " [" + t.priority() + "] " + t.title())
                .collect(Collectors.joining("\n"));

        return "Successfully created " + tickets.size() + " tickets:\n" + ticketSummary;
    }
}
