package TicketCodeIA.agent;

import TicketCodeIA.dto.TicketResponse;
import TicketCodeIA.entity.Ticket;
import TicketCodeIA.enums.AgentType;
import TicketCodeIA.enums.Priority;
import TicketCodeIA.enums.TicketStatus;
import TicketCodeIA.repository.TicketRepository;
import TicketCodeIA.service.AgentLogService;
import TicketCodeIA.service.SseService;
import TicketCodeIA.dto.SseEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class POAgent {

    private final ChatClient.Builder chatClientBuilder;
    private final TicketRepository ticketRepository;
    private final AgentLogService agentLogService;
    private final SseService sseService;
    private final ObjectMapper objectMapper;

    public List<TicketResponse> generateTicketsFromRequirements(String requirements) {
        log.info("PO Agent: Processing requirements to generate tickets");

        String prompt = """
            You are a Product Owner. Break these requirements into tickets.
            Return ONLY a valid JSON array with objects containing: title, description, priority.
            Priority must be one of: LOW, MEDIUM, HIGH, CRITICAL.
            Do not include any explanation, only the JSON array.

            Requirements:
            %s

            Example response format:
            [{"title":"Implement login","description":"Create login page with email/password","priority":"HIGH"}]
            """.formatted(requirements);

        try {
            ChatClient chatClient = chatClientBuilder.build();
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            log.info("PO Agent: Received response from Claude");

            String jsonContent = extractJson(response);
            List<Map<String, String>> ticketData = objectMapper.readValue(
                    jsonContent,
                    new TypeReference<List<Map<String, String>>>() {}
            );

            List<Ticket> createdTickets = new ArrayList<>();

            for (Map<String, String> data : ticketData) {
                Ticket ticket = Ticket.builder()
                        .title(data.get("title"))
                        .description(data.get("description"))
                        .priority(parsePriority(data.get("priority")))
                        .status(TicketStatus.TODO)
                        .assignedAgent(AgentType.PO)
                        .build();

                Ticket saved = ticketRepository.save(ticket);
                createdTickets.add(saved);

                agentLogService.log(saved.getId(), AgentType.PO, "TICKET_CREATED",
                        "Created ticket: " + saved.getTitle());

                sseService.broadcast(SseEvent.ticketUpdated(
                        saved.getId(),
                        saved.getStatus(),
                        AgentType.PO,
                        "New ticket created: " + saved.getTitle()
                ));
            }

            log.info("PO Agent: Created {} tickets", createdTickets.size());

            return createdTickets.stream()
                    .map(TicketResponse::fromEntity)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("PO Agent: Error generating tickets", e);
            throw new RuntimeException("Failed to generate tickets from requirements: " + e.getMessage());
        }
    }

    private String extractJson(String response) {
        int start = response.indexOf('[');
        int end = response.lastIndexOf(']');
        if (start != -1 && end != -1 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }

    private Priority parsePriority(String priority) {
        if (priority == null) {
            return Priority.MEDIUM;
        }
        try {
            return Priority.valueOf(priority.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Priority.MEDIUM;
        }
    }
}
