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
            return objectMapper.readValue(jsonContent, new TypeReference<>() {});

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
}
