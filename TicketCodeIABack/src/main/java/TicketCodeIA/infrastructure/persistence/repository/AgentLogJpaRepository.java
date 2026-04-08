package TicketCodeIA.infrastructure.persistence.repository;

import TicketCodeIA.infrastructure.persistence.entity.AgentLogJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentLogJpaRepository extends JpaRepository<AgentLogJpaEntity, Long> {

    List<AgentLogJpaEntity> findByTicketIdOrderByTimestampDesc(Long ticketId);

    List<AgentLogJpaEntity> findTop20ByOrderByTimestampDesc();
}
