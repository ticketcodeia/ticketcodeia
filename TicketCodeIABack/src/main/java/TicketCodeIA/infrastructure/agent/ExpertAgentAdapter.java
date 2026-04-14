package TicketCodeIA.infrastructure.agent;

import TicketCodeIA.domain.model.chat.ChatMessage;
import TicketCodeIA.domain.port.in.ExpertAgentPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpertAgentAdapter implements ExpertAgentPort {

    private static final String SYSTEM_PROMPT = """
            You are an Expert Software Architect AI assistant. You help users plan and design large-scale software projects
            like social networks, e-commerce platforms, SaaS applications, etc.

            You MUST follow this conversation flow strictly:

            PHASE 1 - DISCOVERY & PLANNING:
            - Chat with the user to understand their project vision, goals, and requirements
            - Ask clarifying questions about features, scale, tech stack preferences, priorities
            - Help them think through architecture, key modules, and implementation phases
            - Break large projects into logical phases or modules
            - Propose a clear breakdown of features and modules

            PHASE 2 - CONFIRMATION BEFORE CREATING TICKETS:
            - When you have a good understanding of the project, present a summary of all the features/modules you plan to create as tickets
            - Then ASK the user explicitly: "Would you like me to create the development tickets now?"
            - WAIT for the user to confirm (e.g. "yes", "go ahead", "create them", etc.)
            - Do NOT call createTickets until the user explicitly confirms
            - Only after confirmation, call createTickets with the ticketsJson parameter

            PHASE 3 - CONFIRMATION BEFORE STARTING PROJECT:
            - After tickets are created, tell the user how many tickets were created and summarize them
            - Then ASK the user explicitly: "Would you like me to start the project now? The development agents will begin working on the tickets."
            - WAIT for the user to confirm (e.g. "yes", "start", "go ahead", etc.)
            - Do NOT call startProject until the user explicitly confirms
            - Only after confirmation, call the startProject tool

            CRITICAL RULES:
            - NEVER call createTickets without asking the user first and getting confirmation
            - NEVER call startProject without asking the user first and getting confirmation
            - Always ask one question at a time, don't overwhelm the user
            - Respond in the same language the user writes in

            TOOL USAGE:
            1. createTickets - You MUST provide the "ticketsJson" parameter as a JSON array STRING.
               Example: ticketsJson = '[{"title":"Auth module","description":"Build login system","priority":"HIGH","enableCodeReview":false,"enableTesting":false}]'
               IMPORTANT: The ticketsJson value must be a valid JSON array string, not an object.
            2. startProject - No parameters needed. Starts the development pipeline.
            """;

    private final ChatClient.Builder chatClientBuilder;
    private final ExpertAgentTools expertAgentTools;

    /**
     * Send a message to the Expert Agent with the full conversation history.
     * Persistence is handled by the use case layer.
     */
    public String chat(String sessionId, List<ChatMessage> history, Long projectId) {
        log.info("Expert Agent: Chat session={}, projectId={}, historySize={}", sessionId, projectId, history.size());

        List<Message> messages = new ArrayList<>();
        for (ChatMessage msg : history) {
            if ("user".equals(msg.getRole())) {
                messages.add(new UserMessage(msg.getContent()));
            } else {
                messages.add(new AssistantMessage(msg.getContent()));
            }
        }

        try {
            ExpertAgentTools.setCurrentProjectId(projectId);

            ChatClient chatClient = chatClientBuilder.build();

            String response = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .messages(messages)
                    .toolCallbacks(expertAgentTools.getToolCallbacks())
                    .call()
                    .content();

            log.info("Expert Agent: Response generated for session {}", sessionId);
            return response;

        } catch (Exception e) {
            log.error("Expert Agent: Error in chat", e);
            return "I encountered an error processing your request. Please try again.";
        } finally {
            ExpertAgentTools.clearCurrentProjectId();
        }
    }

    @Override
    public Long chooseNextTicket(List<Map<String, String>> allTicketsSummary,
                                  List<Map<String, Object>> todoTickets) {
        log.info("Expert Agent: Choosing next ticket to process from {} TODO tickets", todoTickets.size());

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
                You are an Expert Software Architect deciding which ticket to assign to the Developer Agent next.

                Here are ALL tickets in the project with their current status:
                %s

                Here are the TODO tickets available to pick from:
                %s

                Choose the best next ticket to send to the Developer Agent. Consider:
                - Dependencies: pick tickets that don't depend on incomplete work
                - Priority: prefer higher priority tickets
                - Logical order: build foundational features first (e.g. database models before API endpoints, backend before frontend)

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
            log.info("Expert Agent: Chose ticket {} as next to process", chosenId);
            return chosenId;
        } catch (Exception e) {
            log.warn("Expert Agent: Failed to choose ticket, falling back to first TODO", e);
            return ((Number) todoTickets.get(0).get("id")).longValue();
        }
    }
}
