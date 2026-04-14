package TicketCodeIA.infrastructure.persistence.repository;

import TicketCodeIA.infrastructure.persistence.entity.ChatMessageJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageJpaRepository extends JpaRepository<ChatMessageJpaEntity, Long> {

    List<ChatMessageJpaEntity> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    List<ChatMessageJpaEntity> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    void deleteBySessionId(String sessionId);
}
