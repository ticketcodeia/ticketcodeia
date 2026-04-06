package TicketCodeIA.agent;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import TicketCodeIA.dto.SseEvent;
import TicketCodeIA.entity.Ticket;
import TicketCodeIA.enums.AgentType;
import TicketCodeIA.enums.TicketStatus;
import TicketCodeIA.service.AgentLogService;
import TicketCodeIA.service.SseService;
import TicketCodeIA.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewerAgent {

	private final ChatClient.Builder chatClientBuilder;
	private final TicketService ticketService;
	private final AgentLogService agentLogService;
	private final SseService sseService;
	private final ObjectMapper objectMapper;

	@Value("${tickcode.workspace:/tmp/tickcode-workspace}")
	private String workspacePath;

	public AgentResult process(Ticket ticket) {
		log.info("Reviewer Agent: Reviewing ticket {}", ticket.getId());

		ticket.setAssignedAgent(AgentType.REVIEWER);
		ticket.addAgentLog("Reviewer Agent started code review");
		ticketService.saveTicket(ticket);

		agentLogService.log(ticket.getId(), AgentType.REVIEWER, "STARTED",
				"Started code review for: " + ticket.getTitle());

		sseService.broadcast(SseEvent.ticketUpdated(ticket.getId(), TicketStatus.CODE_REVIEW, AgentType.REVIEWER,
				"Code review in progress"));

		try {
			String codeToReview = gatherRecentCode();

			String prompt = """
					You are a code reviewer. Review the following code for bugs, security issues, and clean code practices.

					Respond with ONLY a valid JSON object in this exact format:
					{"decision": "APPROVED" or "CHANGES_REQUESTED", "comments": "your review comments"}

					Code to review:
					%s

					Ticket context:
					Title: %s
					Description: %s
					"""
					.formatted(codeToReview, ticket.getTitle(), ticket.getDescription());

			ChatClient chatClient = chatClientBuilder.build();
			String response = chatClient.prompt().user(prompt).call().content();

			log.info("Reviewer Agent: Received review response");

			String jsonContent = extractJson(response);
			JsonNode reviewResult = objectMapper.readTree(jsonContent);

			String decision = reviewResult.has("decision") ? reviewResult.get("decision").asText()
					: "CHANGES_REQUESTED";
			String comments = reviewResult.has("comments") ? reviewResult.get("comments").asText()
					: "No comments provided";

			ticket.addAgentLog("Code review result: " + decision + " - " + comments);

			if ("APPROVED".equalsIgnoreCase(decision)) {
				ticket.setStatus(TicketStatus.TESTING);
				ticketService.saveTicket(ticket);

				agentLogService.log(ticket.getId(), AgentType.REVIEWER, "APPROVED", "Code review passed: " + comments);

				sseService.broadcast(SseEvent.ticketUpdated(ticket.getId(), TicketStatus.TESTING, AgentType.REVIEWER,
						"Code review approved, moving to testing"));

				return AgentResult.success("Code review approved: " + comments);
			} else {
				agentLogService.log(ticket.getId(), AgentType.REVIEWER, "CHANGES_REQUESTED",
						"Changes requested: " + comments);

				sseService.broadcast(SseEvent.ticketUpdated(ticket.getId(), TicketStatus.CODE_REVIEW,
						AgentType.REVIEWER, "Changes requested by reviewer"));

				return AgentResult.needsChanges(comments);
			}

		} catch (Exception e) {
			log.error("Reviewer Agent: Error reviewing ticket {}", ticket.getId(), e);

			ticket.addAgentLog("Reviewer Agent error: " + e.getMessage());
			ticketService.saveTicket(ticket);

			agentLogService.log(ticket.getId(), AgentType.REVIEWER, "ERROR", e.getMessage());

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
