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
 * Tool callback that lets the Expert Agent assign a ticket to a specific agent or human.
 */
@Slf4j
public class AssignTicketTool implements ToolCallback {

    private static final String INPUT_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "ticketId": {
                  "type": "integer",
                  "description": "The ID of the ticket to assign"
                },
                "assignTo": {
                  "type": "string",
                  "enum": ["DEVELOPER", "TESTER", "REVIEWER", "HUMAN"],
                  "description": "Who to assign the ticket to: DEVELOPER (AI dev agent), TESTER (AI test agent), REVIEWER (AI code review agent), or HUMAN (human developer)"
                },
                "reason": {
                  "type": "string",
                  "description": "Why the ticket is being assigned to this agent"
                }
              },
              "required": ["ticketId", "assignTo"]
            }
            """;

    private final ToolDefinition toolDefinition = DefaultToolDefinition.builder()
            .name("assignTicket")
            .description("Assign a ticket to a specific agent or human. "
                    + "Use DEVELOPER to assign to the AI Developer Agent, "
                    + "TESTER to assign to the AI Tester Agent, "
                    + "REVIEWER to assign to the AI Code Reviewer Agent, "
                    + "or HUMAN to assign to a human developer. "
                    + "When assigning to HUMAN, the ticket moves to the human board.")
            .inputSchema(INPUT_SCHEMA)
            .build();

    private final TicketRepositoryPort ticketRepository;
    private final AgentLogRepositoryPort agentLogRepository;
    private final EventPublisherPort eventPublisher;
    private final ObjectMapper objectMapper;

    public AssignTicketTool(TicketRepositoryPort ticketRepository,
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
        log.info("assignTicket raw input: {}", toolInput);
        return execute(toolInput);
    }

    private String execute(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);

            if (!root.has("ticketId") || !root.has("assignTo")) {
                return "Error: ticketId and assignTo are required.";
            }

            long ticketId = root.get("ticketId").asLong();
            String assignToStr = root.get("assignTo").asText();
            String reason = root.has("reason") ? root.get("reason").asText() : "Assigned by Expert Agent";

            AgentType assignTo;
            try {
                assignTo = AgentType.valueOf(assignToStr.toUpperCase().trim());
            } catch (IllegalArgumentException e) {
                return "Error: Invalid agent '" + assignToStr + "'. Valid values: DEVELOPER, TESTER, REVIEWER, HUMAN";
            }

            Ticket ticket = ticketRepository.findByIdWithProject(ticketId).orElse(null);
            if (ticket == null) {
                return "Error: Ticket #" + ticketId + " not found.";
            }

            AgentType previousAgent = ticket.getAssignedAgent();
            TicketStatus previousStatus = ticket.getStatus();
            ticket.setAssignedAgent(assignTo);

            // Auto-adjust status based on assignment
            if (assignTo == AgentType.HUMAN) {
                if (!ticket.isOnHumanBoard()) {
                    ticket.setStatus(TicketStatus.HUMAN_TODO);
                }
            } else if (assignTo == AgentType.DEVELOPER && ticket.getStatus() == TicketStatus.TODO) {
                ticket.setStatus(TicketStatus.IN_PROGRESS);
            } else if (assignTo == AgentType.REVIEWER && ticket.getStatus() == TicketStatus.IN_PROGRESS) {
                ticket.setStatus(TicketStatus.CODE_REVIEW);
            } else if (assignTo == AgentType.TESTER && ticket.getStatus() == TicketStatus.CODE_REVIEW) {
                ticket.setStatus(TicketStatus.TESTING);
            }

            ticketRepository.save(ticket);

            String previousAgentName = previousAgent != null ? previousAgent.name() : "none";
            agentLogRepository.save(AgentLog.create(
                    ticketId, AgentType.EXPERT, "TICKET_ASSIGNED",
                    "Assigned from " + previousAgentName + " to " + assignTo + ": " + reason));

            eventPublisher.publish(new TicketStatusChangedEvent(
                    ticketId, previousStatus, ticket.getStatus(), AgentType.EXPERT,
                    "Assigned to " + assignTo + ": " + reason));

            log.info("assignTicket: Ticket #{} assigned to {} (status: {})", ticketId, assignTo, ticket.getStatus());

            String statusNote = previousStatus != ticket.getStatus()
                    ? " Status changed from " + previousStatus + " to " + ticket.getStatus() + "."
                    : "";

            return "Ticket #" + ticketId + " assigned to " + assignTo + "." + statusNote;

        } catch (Exception e) {
            log.error("assignTicket error: {}", rawJson, e);
            return "Error assigning ticket: " + e.getMessage();
        }
    }
}
