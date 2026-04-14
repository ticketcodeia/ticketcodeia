package TicketCodeIA.infrastructure.persistence.mapper;

import TicketCodeIA.domain.model.chat.ChatMessage;
import TicketCodeIA.infrastructure.persistence.entity.ChatMessageJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ChatMessagePersistenceMapper {

    public ChatMessage toDomain(ChatMessageJpaEntity entity) {
        return new ChatMessage(
                entity.getId(),
                entity.getSessionId(),
                entity.getProjectId(),
                entity.getRole(),
                entity.getContent(),
                entity.getCreatedAt()
        );
    }

    public ChatMessageJpaEntity toJpaEntity(ChatMessage domain) {
        return ChatMessageJpaEntity.builder()
                .id(domain.getId())
                .sessionId(domain.getSessionId())
                .projectId(domain.getProjectId())
                .role(domain.getRole())
                .content(domain.getContent())
                .build();
    }
}
