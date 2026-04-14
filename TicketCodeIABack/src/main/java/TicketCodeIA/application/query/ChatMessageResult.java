package TicketCodeIA.application.query;

import TicketCodeIA.domain.model.chat.ChatMessage;

import java.time.LocalDateTime;

public record ChatMessageResult(
        Long id,
        String sessionId,
        Long projectId,
        String role,
        String content,
        LocalDateTime createdAt
) {
    public static ChatMessageResult fromDomain(ChatMessage message) {
        return new ChatMessageResult(
                message.getId(),
                message.getSessionId(),
                message.getProjectId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
