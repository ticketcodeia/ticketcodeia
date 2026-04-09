package TicketCodeIA.infrastructure.agent;

import TicketCodeIA.domain.port.in.POAgentPort;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class POAgentAdapter implements POAgentPort {

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public List<Map<String, String>> generateTicketData(String requirements) {
        log.info("PO Agent: Processing requirements to generate tickets");

        String prompt = """
                You are a Product Owner. Break these requirements into medium-sized tickets.

                IMPORTANT: Create medium-sized tickets, NOT small atomic tasks.
                Each ticket should represent a meaningful feature or module that takes real effort to implement.
                Group related work together into a single ticket rather than splitting into many tiny tasks.
                For example, "Implement user authentication with login, registration, and password reset" is ONE ticket,
                not three separate tickets. A ticket should cover a full functional area, not a single file or function change.
                Aim for 3-6 tickets for a typical project, not 10-20.

                Each ticket description should be detailed and include:
                - What needs to be built
                - Key technical requirements
                - Acceptance criteria

                Return ONLY a valid JSON array with objects containing: title, description, priority, enableCodeReview, enableTesting.
                Priority must be one of: LOW, MEDIUM, HIGH, CRITICAL.
                enableCodeReview and enableTesting are booleans indicating whether code review and testing agents should be activated for this ticket.
                By default, set both enableCodeReview and enableTesting to false unless the requirements explicitly mention needing code review or testing.
                Do not include any explanation, only the JSON array.

                Requirements:
                %s

                Example response format:
                [{"title":"Implement user authentication module","description":"Build complete authentication system including login page with email/password, registration form with validation, password reset flow via email, and JWT token management. Must include proper error handling and secure password hashing.","priority":"HIGH","enableCodeReview":false,"enableTesting":false}]
                """.formatted(requirements);

        try {
            ChatClient chatClient = chatClientBuilder.build();
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            log.info("PO Agent: Received response from Claude");

            String jsonContent = extractJson(response);
            return objectMapper.readValue(jsonContent, new TypeReference<>() {});

        } catch (Exception e) {
            log.error("PO Agent: Error generating tickets", e);
            throw new RuntimeException("Failed to generate tickets from requirements: " + e.getMessage());
        }
    }

    @Override
    public Long chooseNextTicket(List<Map<String, String>> allTicketsSummary,
                                  List<Map<String, Object>> todoTickets) {
        log.info("PO Agent: Choosing next ticket to process from {} TODO tickets", todoTickets.size());

        if (todoTickets.size() == 1) {
            return ((Number) todoTickets.get(0).get("id")).longValue();
        }

        StringBuilder allTicketsInfo = new StringBuilder();
        for (Map<String, String> t : allTicketsSummary) {
            allTicketsInfo.append("- [").append(t.get("status")).append("] ").append(t.get("title")).append("\n");
        }

        StringBuilder todoInfo = new StringBuilder();
        for (Map<String, Object> t : todoTickets) {
            todoInfo.append("- ID=").append(t.get("id")).append(": ").append(t.get("title")).append("\n");
        }

        String prompt = """
                You are a Product Owner deciding which ticket to work on next.

                Here are ALL tickets in the project with their current status:
                %s

                Here are the TODO tickets available to pick from:
                %s

                Choose the best next ticket to move to development. Consider:
                - Dependencies: pick tickets that don't depend on incomplete work
                - Priority: prefer higher priority tickets
                - Logical order: build foundational features first

                Return ONLY the numeric ID of the chosen ticket. Nothing else.
                """.formatted(allTicketsInfo, todoInfo);

        try {
            ChatClient chatClient = chatClientBuilder.build();
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            String cleaned = response.trim().replaceAll("[^0-9]", "");
            Long chosenId = Long.parseLong(cleaned);
            log.info("PO Agent: Chose ticket {} as next to process", chosenId);
            return chosenId;
        } catch (Exception e) {
            log.warn("PO Agent: Failed to choose ticket, falling back to first TODO", e);
            return ((Number) todoTickets.get(0).get("id")).longValue();
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
}
