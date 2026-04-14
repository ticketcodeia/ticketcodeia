package TicketCodeIA.domain.port.out;

import TicketCodeIA.domain.model.chat.ChatMessage;

import java.util.List;

public interface ChatMessageRepositoryPort {

    ChatMessage save(ChatMessage message);

    List<ChatMessage> findBySessionIdOrderByCreatedAt(String sessionId);

    List<ChatMessage> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    void deleteBySessionId(String sessionId);
}
