package TicketCodeIA.infrastructure.persistence.mapper;

import TicketCodeIA.domain.model.agentlog.AgentLog;
import TicketCodeIA.infrastructure.persistence.entity.AgentLogJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class AgentLogPersistenceMapper {

    public AgentLog toDomain(AgentLogJpaEntity entity) {
        return new AgentLog(
                entity.getId(),
                entity.getTicketId(),
                entity.getAgentType(),
                entity.getAction(),
                entity.getMessage(),
                entity.getTimestamp()
        );
    }

    public AgentLogJpaEntity toJpaEntity(AgentLog domain) {
        return AgentLogJpaEntity.builder()
                .id(domain.getId())
                .ticketId(domain.getTicketId())
                .agentType(domain.getAgentType())
                .action(domain.getAction())
                .message(domain.getMessage())
                .build();
    }
}
