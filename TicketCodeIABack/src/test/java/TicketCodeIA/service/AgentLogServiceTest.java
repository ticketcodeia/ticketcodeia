package TicketCodeIA.service;

import TicketCodeIA.entity.AgentLog;
import TicketCodeIA.enums.AgentType;
import TicketCodeIA.repository.AgentLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentLogServiceTest {

    @Mock
    private AgentLogRepository agentLogRepository;

    @InjectMocks
    private AgentLogService agentLogService;

    @Test
    void log_savesAgentLogWithCorrectFields() {
        AgentLog saved = AgentLog.builder()
                .id(1L)
                .ticketId(10L)
                .agentType(AgentType.DEVELOPER)
                .action("STARTED")
                .message("Development started")
                .build();
        when(agentLogRepository.save(any(AgentLog.class))).thenReturn(saved);

        AgentLog result = agentLogService.log(10L, AgentType.DEVELOPER, "STARTED", "Development started");

        ArgumentCaptor<AgentLog> captor = ArgumentCaptor.forClass(AgentLog.class);
        verify(agentLogRepository).save(captor.capture());
        AgentLog captured = captor.getValue();
        assertThat(captured.getTicketId()).isEqualTo(10L);
        assertThat(captured.getAgentType()).isEqualTo(AgentType.DEVELOPER);
        assertThat(captured.getAction()).isEqualTo("STARTED");
        assertThat(captured.getMessage()).isEqualTo("Development started");
        assertThat(result).isEqualTo(saved);
    }

    @Test
    void getLogsByTicketId_delegatesToRepository() {
        List<AgentLog> logs = List.of(
                AgentLog.builder().id(1L).ticketId(5L).build(),
                AgentLog.builder().id(2L).ticketId(5L).build()
        );
        when(agentLogRepository.findByTicketIdOrderByTimestampDesc(5L)).thenReturn(logs);

        List<AgentLog> result = agentLogService.getLogsByTicketId(5L);

        assertThat(result).hasSize(2);
        verify(agentLogRepository).findByTicketIdOrderByTimestampDesc(5L);
    }

    @Test
    void getRecentLogs_delegatesToRepository() {
        List<AgentLog> recentLogs = List.of(AgentLog.builder().id(1L).build());
        when(agentLogRepository.findTop20ByOrderByTimestampDesc()).thenReturn(recentLogs);

        List<AgentLog> result = agentLogService.getRecentLogs();

        assertThat(result).hasSize(1);
        verify(agentLogRepository).findTop20ByOrderByTimestampDesc();
    }

    @Test
    void log_withDifferentAgentTypes() {
        when(agentLogRepository.save(any())).thenReturn(AgentLog.builder().build());

        agentLogService.log(1L, AgentType.REVIEWER, "APPROVED", "Code looks good");
        agentLogService.log(1L, AgentType.TESTER, "COMPLETED", "All tests passed");

        verify(agentLogRepository, times(2)).save(any());
    }
}
