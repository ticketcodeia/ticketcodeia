package TicketCodeIA.infrastructure.persistence.adapter;

import TicketCodeIA.domain.model.agentlog.AgentLog;
import TicketCodeIA.domain.port.out.AgentLogRepositoryPort;
import TicketCodeIA.infrastructure.persistence.mapper.AgentLogPersistenceMapper;
import TicketCodeIA.infrastructure.persistence.repository.AgentLogJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AgentLogRepositoryAdapter implements AgentLogRepositoryPort {

    private final AgentLogJpaRepository jpaRepository;
    private final AgentLogPersistenceMapper mapper;

    @Override
    public AgentLog save(AgentLog agentLog) {
        var entity = mapper.toJpaEntity(agentLog);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<AgentLog> findByTicketIdOrderByTimestampDesc(Long ticketId) {
        return jpaRepository.findByTicketIdOrderByTimestampDesc(ticketId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<AgentLog> findRecentLogs() {
        return jpaRepository.findTop20ByOrderByTimestampDesc().stream()
                .map(mapper::toDomain).toList();
    }
}
