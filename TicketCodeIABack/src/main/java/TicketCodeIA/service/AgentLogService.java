package TicketCodeIA.service;

import TicketCodeIA.entity.AgentLog;
import TicketCodeIA.enums.AgentType;
import TicketCodeIA.repository.AgentLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentLogService {

    private final AgentLogRepository agentLogRepository;

    @Transactional
    public AgentLog log(Long ticketId, AgentType agentType, String action, String message) {
        AgentLog agentLog = AgentLog.builder()
                .ticketId(ticketId)
                .agentType(agentType)
                .action(action)
                .message(message)
                .build();
        return agentLogRepository.save(agentLog);
    }

    public List<AgentLog> getLogsByTicketId(Long ticketId) {
        return agentLogRepository.findByTicketIdOrderByTimestampDesc(ticketId);
    }

    public List<AgentLog> getRecentLogs() {
        return agentLogRepository.findTop20ByOrderByTimestampDesc();
    }
}
