package TicketCodeIA.infrastructure.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.lang.Nullable;

import TicketCodeIA.application.port.out.EventPublisherPort;
import TicketCodeIA.domain.enums.AgentType;
import TicketCodeIA.domain.enums.TicketStatus;
import TicketCodeIA.domain.event.TicketStatusChangedEvent;
import TicketCodeIA.domain.model.agentlog.AgentLog;
import TicketCodeIA.domain.model.ticket.Ticket;
import TicketCodeIA.domain.port.out.AgentLogRepositoryPort;
import TicketCodeIA.domain.port.out.TicketRepositoryPort;
import lombok.extern.slf4j.Slf4j;

/**
 * Tool callback that lets the Expert Agent change the status of a ticket.
 */
@Slf4j
public class ChangeTicketStatusTool implements ToolCallback {

    private static final String INPUT_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "ticketId": {
                  "type": "integer",
                  "description": "The ID of the ticket to update"
                },
                "status": {
                  "type": "string",
                  "enum": ["TODO", "IN_PROGRESS", "CODE_REVIEW", "TESTING", "DONE", "ESCALATED", "HUMAN_TODO", "HUMAN_DEV", "HUMAN_REVIEW", "HUMAN_TESTING"],
                  "description": "The new status for the ticket"
                },
                "reason": {
                  "type": "string",
                  "description": "Why the status is being changed"
                }
              },
              "required": ["ticketId", "status"]
            }
            """;

    private final ToolDefinition toolDefinition = DefaultToolDefinition.builder()
            .name("changeTicketStatus")
            .description("Change the status of a ticket. "
                    + "Available statuses: TODO, IN_PROGRESS, CODE_REVIEW, TESTING, DONE, ESCALATED, "
                    + "HUMAN_TODO, HUMAN_DEV, HUMAN_REVIEW, HUMAN_TESTING. "
                    + "Use this when the user asks to move a ticket to a different stage.")
            .inputSchema(INPUT_SCHEMA)
            .build();

    private final TicketRepositoryPort ticketRepository;
    private final AgentLogRepositoryPort agentLogRepository;
    private final EventPublisherPort eventPublisher;
    private final ObjectMapper objectMapper;

    public ChangeTicketStatusTool(TicketRepositoryPort ticketRepository,
                                  AgentLogRepositoryPort agentLogRepository,
                                  EventPublisherPort eventPublisher,
                                  ObjectMapper objectMapper) {
        this.ticketRepository = ticketRepository;
        this.agentLogRepository = agentLogRepository;
        this.eventPublisher = eventPublisher;
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
        log.info("changeTicketStatus raw input: {}", toolInput);
        return execute(toolInput);
    }

    private String execute(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);

            if (!root.has("ticketId") || !root.has("status")) {
                return "Error: ticketId and status are required.";
            }

            long ticketId = root.get("ticketId").asLong();
            String statusStr = root.get("status").asText();
            String reason = root.has("reason") ? root.get("reason").asText() : "Status changed by Expert Agent";

            TicketStatus newStatus;
            try {
                newStatus = TicketStatus.valueOf(statusStr.toUpperCase().trim());
            } catch (IllegalArgumentException e) {
                return "Error: Invalid status '" + statusStr + "'. Valid values: TODO, IN_PROGRESS, CODE_REVIEW, TESTING, DONE, ESCALATED, HUMAN_TODO, HUMAN_DEV, HUMAN_REVIEW, HUMAN_TESTING";
            }

            Ticket ticket = ticketRepository.findByIdWithProject(ticketId).orElse(null);
            if (ticket == null) {
                return "Error: Ticket #" + ticketId + " not found.";
            }

            TicketStatus previousStatus = ticket.getStatus();
            ticket.setStatus(newStatus);
            ticketRepository.save(ticket);

            agentLogRepository.save(AgentLog.create(
                    ticketId, AgentType.EXPERT, "STATUS_CHANGED",
                    "Status changed from " + previousStatus + " to " + newStatus + ": " + reason));

            eventPublisher.publish(new TicketStatusChangedEvent(
                    ticketId, previousStatus, newStatus, AgentType.EXPERT, reason));

            log.info("changeTicketStatus: Ticket #{} status changed {} -> {}", ticketId, previousStatus, newStatus);
            return "Ticket #" + ticketId + " status changed from " + previousStatus + " to " + newStatus + ".";

        } catch (Exception e) {
            log.error("changeTicketStatus error: {}", rawJson, e);
            return "Error changing ticket status: " + e.getMessage();
        }
    }
}
