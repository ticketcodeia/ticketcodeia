package TicketCodeIA.infrastructure.agent;

import TicketCodeIA.domain.model.ticket.Ticket;
import TicketCodeIA.domain.port.in.ReviewerAgentPort;
import TicketCodeIA.domain.valueobject.AgentResult;
import TicketCodeIA.domain.valueobject.ProjectContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewerAgentAdapter implements ReviewerAgentPort {

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${tickcode.workspace:/tmp/tickcode-workspace}")
    private String workspacePath;

    @Override
    public AgentResult process(Ticket ticket, ProjectContext context) {
        log.info("Reviewer Agent: Reviewing ticket {} with project context", ticket.getId());

        try {
            String codeToReview = gatherRecentCode();

            String prompt = """
                    You are a code reviewer. Review the following code for bugs, security issues, and clean code practices.

                    %s

                    Respond with ONLY a valid JSON object in this exact format:
                    {"decision": "APPROVED" or "CHANGES_REQUESTED", "comments": "your review comments"}

                    Code to review:
                    %s

                    Current ticket being reviewed:
                    Ticket #%d: %s
                    Description: %s
                    """.formatted(context.toPromptString(), codeToReview,
                    ticket.getId(), ticket.getTitle(), ticket.getDescription());

            ChatClient chatClient = chatClientBuilder.build();
            String response = chatClient.prompt().user(prompt).call().content();

            log.info("Reviewer Agent: Received review response");

            String jsonContent = extractJson(response);
            JsonNode reviewResult = objectMapper.readTree(jsonContent);

            String decision = reviewResult.has("decision")
                    ? reviewResult.get("decision").asText() : "CHANGES_REQUESTED";
            String comments = reviewResult.has("comments")
                    ? reviewResult.get("comments").asText() : "No comments provided";

            if ("APPROVED".equalsIgnoreCase(decision)) {
                return AgentResult.success("Code review approved: " + comments);
            } else {
                return AgentResult.needsChanges(comments);
            }

        } catch (Exception e) {
            log.error("Reviewer Agent: Error reviewing ticket {}", ticket.getId(), e);
            return AgentResult.failure(e.getMessage());
        }
    }

    private String gatherRecentCode() {
        try {
            File workspace = new File(workspacePath);
            if (!workspace.exists()) {
                return "No code files found in workspace";
            }

            try (Stream<Path> paths = Files.walk(workspace.toPath())) {
                return paths.filter(Files::isRegularFile).filter(p -> {
                    String name = p.getFileName().toString();
                    return name.endsWith(".java") || name.endsWith(".ts") || name.endsWith(".js")
                            || name.endsWith(".py") || name.endsWith(".html") || name.endsWith(".css");
                }).limit(10).map(p -> {
                    try {
                        String content = Files.readString(p);
                        if (content.length() > 2000) {
                            content = content.substring(0, 2000) + "\n... (truncated)";
                        }
                        return "=== " + p.getFileName() + " ===\n" + content;
                    } catch (Exception e) {
                        return "";
                    }
                }).collect(Collectors.joining("\n\n"));
            }
        } catch (Exception e) {
            log.error("Error gathering code for review", e);
            return "Error reading workspace files";
        }
    }

    private String extractJson(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start != -1 && end != -1 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }
}
