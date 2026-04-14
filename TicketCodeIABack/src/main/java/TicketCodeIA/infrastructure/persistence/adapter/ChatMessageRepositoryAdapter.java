package TicketCodeIA.infrastructure.persistence.adapter;

import TicketCodeIA.domain.model.chat.ChatMessage;
import TicketCodeIA.domain.port.out.ChatMessageRepositoryPort;
import TicketCodeIA.infrastructure.persistence.mapper.ChatMessagePersistenceMapper;
import TicketCodeIA.infrastructure.persistence.repository.ChatMessageJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ChatMessageRepositoryAdapter implements ChatMessageRepositoryPort {

    private final ChatMessageJpaRepository jpaRepository;
    private final ChatMessagePersistenceMapper mapper;

    @Override
    public ChatMessage save(ChatMessage message) {
        var entity = mapper.toJpaEntity(message);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<ChatMessage> findBySessionIdOrderByCreatedAt(String sessionId) {
        return jpaRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<ChatMessage> findByProjectIdOrderByCreatedAtDesc(Long projectId) {
        return jpaRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    @Transactional
    public void deleteBySessionId(String sessionId) {
        jpaRepository.deleteBySessionId(sessionId);
    }
}
