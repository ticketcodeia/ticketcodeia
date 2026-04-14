package TicketCodeIA.application.usecase.chat;

import TicketCodeIA.application.query.ChatMessageResult;
import TicketCodeIA.domain.model.chat.ChatMessage;
import TicketCodeIA.domain.port.in.ExpertAgentPort;
import TicketCodeIA.domain.port.out.ChatMessageRepositoryPort;
import TicketCodeIA.infrastructure.agent.ExpertAgentAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SendChatMessageUseCase {

    private final ChatMessageRepositoryPort chatMessageRepository;
    private final ExpertAgentAdapter expertAgent;

    public ChatMessageResult execute(String sessionId, String userMessage, Long projectId) {
        log.info("SendChatMessage: session={}, projectId={}", sessionId, projectId);

        // Persist user message
        chatMessageRepository.save(ChatMessage.createUser(sessionId, projectId, userMessage));

        // Load full conversation history from DB
        List<ChatMessage> history = chatMessageRepository.findBySessionIdOrderByCreatedAt(sessionId);

        // Send to Expert Agent (AI call with history)
        String response = expertAgent.chat(sessionId, history, projectId);

        // Persist assistant response
        ChatMessage saved = chatMessageRepository.save(
                ChatMessage.createAssistant(sessionId, projectId, response));

        log.info("SendChatMessage: response generated for session {}", sessionId);
        return ChatMessageResult.fromDomain(saved);
    }
}
