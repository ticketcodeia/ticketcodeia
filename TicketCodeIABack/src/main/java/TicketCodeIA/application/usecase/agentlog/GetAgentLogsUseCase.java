package TicketCodeIA.application.usecase.agentlog;

import TicketCodeIA.application.query.AgentLogResult;
import TicketCodeIA.domain.port.out.AgentLogRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetAgentLogsUseCase {

    private final AgentLogRepositoryPort agentLogRepository;

    @Transactional(readOnly = true)
    public List<AgentLogResult> getByTicketId(Long ticketId) {
        return agentLogRepository.findByTicketIdOrderByTimestampDesc(ticketId).stream()
                .map(AgentLogResult::fromDomain)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AgentLogResult> getRecentLogs() {
        return agentLogRepository.findRecentLogs().stream()
                .map(AgentLogResult::fromDomain)
                .toList();
    }
}
