package TicketCodeIA.agent;

import TicketCodeIA.entity.Ticket;
import TicketCodeIA.enums.AgentType;
import TicketCodeIA.enums.Priority;
import TicketCodeIA.enums.TicketStatus;
import TicketCodeIA.service.AgentLogService;
import TicketCodeIA.service.SseService;
import TicketCodeIA.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TesterAgentTest {

    @Mock private TicketService ticketService;
    @Mock private AgentLogService agentLogService;
    @Mock private SseService sseService;

    @InjectMocks
    private TesterAgent testerAgent;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(testerAgent, "workspacePath",
                System.getProperty("java.io.tmpdir") + "/tickcode-test");
        ReflectionTestUtils.setField(testerAgent, "maxTurns", 5);
        lenient().when(ticketService.saveTicket(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(agentLogService.log(anyLong(), any(), anyString(), anyString())).thenReturn(null);
        lenient().doNothing().when(sseService).broadcast(any());
    }

    private Ticket buildTicket() {
        return Ticket.builder()
                .id(2L)
                .title("Test authentication")
                .description("Write tests for JWT auth")
                .status(TicketStatus.TESTING)
                .priority(Priority.MEDIUM)
                .build();
    }

    @Test
    void process_setsAssignedAgentToTester() {
        Ticket ticket = buildTicket();

        testerAgent.process(ticket);

        assertThat(ticket.getAssignedAgent()).isEqualTo(AgentType.TESTER);
        verify(ticketService, atLeastOnce()).saveTicket(any());
    }

    @Test
    void process_broadcastsSseEvent() {
        Ticket ticket = buildTicket();

        testerAgent.process(ticket);

        verify(sseService, atLeastOnce()).broadcast(any());
    }

    @Test
    void process_logsToAgentLog() {
        Ticket ticket = buildTicket();

        testerAgent.process(ticket);

        verify(agentLogService, atLeastOnce()).log(eq(2L), eq(AgentType.TESTER), anyString(), anyString());
    }

    @Test
    void process_addsTesterStartedLog() {
        Ticket ticket = buildTicket();

        testerAgent.process(ticket);

        assertThat(ticket.getAgentLogs())
                .anyMatch(log -> log.contains("Tester Agent started testing"));
    }

    @Test
    void process_whenClaudeNotAvailable_returnsResult() {
        Ticket ticket = buildTicket();

        AgentResult result = testerAgent.process(ticket);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess() || result.isFailure()).isTrue();
    }
}
