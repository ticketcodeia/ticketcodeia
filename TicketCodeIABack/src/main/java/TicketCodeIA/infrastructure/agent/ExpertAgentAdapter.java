package TicketCodeIA.infrastructure.agent;

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
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpertAgentAdapter {

    private static final String SYSTEM_PROMPT = """
            You are an Expert Software Architect AI assistant. You help users plan and design large-scale software projects
            like social networks, e-commerce platforms, SaaS applications, etc.

            Your role:
            1. CHAT with the user to understand their project vision, goals, and requirements
            2. ASK clarifying questions about features, scale, tech stack preferences, priorities
            3. HELP them think through architecture, key modules, and implementation phases
            4. Propose a breakdown of the project into key modules and features
            5. When you have gathered enough information and the user confirms, use the createTickets tool to create development tickets

            Guidelines:
            - Be conversational and helpful, guide the user through the planning process
            - Break large projects into logical phases or modules
            - Ask about priorities, MVP features vs nice-to-haves
            - Give concrete suggestions and architecture recommendations
            - When you have a good understanding, propose a clear list of features/modules
            - When the user agrees with your proposal or asks you to create tickets, call the createTickets tool with detailed requirements
            - Respond in the same language the user writes in

            IMPORTANT: You have access to a createTickets tool. When you have gathered enough requirements and the user
            confirms they want to proceed, call this tool with a detailed requirements document. The tool will contact
            the PO Agent to create the development tickets automatically.
            """;

    private final ChatClient.Builder chatClientBuilder;
    private final ExpertAgentTools expertAgentTools;

    private final Map<String, List<Message>> conversationHistory = new ConcurrentHashMap<>();

    public String chat(String sessionId, String userMessage, Long projectId) {
        log.info("Expert Agent: Chat session={}, projectId={}", sessionId, projectId);

        List<Message> history = conversationHistory.computeIfAbsent(sessionId, k -> new ArrayList<>());
        history.add(new UserMessage(userMessage));

        try {
            // Set projectId in ThreadLocal so the @Tool method can access it
            ExpertAgentTools.setCurrentProjectId(projectId);

            ChatClient chatClient = chatClientBuilder.build();

            String response = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .messages(history)
                    .tools(expertAgentTools)
                    .call()
                    .content();

            history.add(new AssistantMessage(response));

            log.info("Expert Agent: Response generated for session {}", sessionId);
            return response;

        } catch (Exception e) {
            log.error("Expert Agent: Error in chat", e);
            String errorMsg = "I encountered an error processing your request. Please try again.";
            history.add(new AssistantMessage(errorMsg));
            return errorMsg;
        } finally {
            ExpertAgentTools.clearCurrentProjectId();
        }
    }

    public void clearSession(String sessionId) {
        conversationHistory.remove(sessionId);
        log.info("Expert Agent: Cleared session {}", sessionId);
    }
}
