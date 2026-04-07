package TicketCodeIA.agent;

import TicketCodeIA.entity.Ticket;
import TicketCodeIA.enums.AgentType;
import TicketCodeIA.enums.Priority;
import TicketCodeIA.enums.TicketStatus;
import TicketCodeIA.service.AgentLogService;
import TicketCodeIA.service.SseService;
import TicketCodeIA.service.TicketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewerAgentTest {

    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    private ChatClient.Builder chatClientBuilder;

    @Mock private TicketService ticketService;
    @Mock private AgentLogService agentLogService;
    @Mock private SseService sseService;

    private ReviewerAgent reviewerAgent;

    @BeforeEach
    void setUp() {
        ObjectMapper realMapper = new ObjectMapper();
        reviewerAgent = new ReviewerAgent(chatClientBuilder, ticketService, agentLogService, sseService, realMapper);
        ReflectionTestUtils.setField(reviewerAgent, "workspacePath", System.getProperty("java.io.tmpdir") + "/test-review");
        lenient().when(ticketService.saveTicket(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(agentLogService.log(anyLong(), any(), anyString(), anyString())).thenReturn(null);
        lenient().doNothing().when(sseService).broadcast(any());
    }

    private Ticket buildTicket() {
        return Ticket.builder()
                .id(1L)
                .title("Implement login")
                .description("Create login page with email/password auth")
                .status(TicketStatus.CODE_REVIEW)
                .priority(Priority.HIGH)
                .build();
    }

    @Test
    void process_whenApproved_returnsSuccessAndMovesToTesting() {
        String aiResponse = "{\"decision\": \"APPROVED\", \"comments\": \"Code looks great!\"}";
        when(chatClientBuilder.build().prompt().user(anyString()).call().content()).thenReturn(aiResponse);

        Ticket ticket = buildTicket();
        AgentResult result = reviewerAgent.process(ticket);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).contains("approved");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.TESTING);
        verify(agentLogService, atLeastOnce()).log(eq(1L), eq(AgentType.REVIEWER), eq("APPROVED"), anyString());
    }

    @Test
    void process_whenChangesRequested_returnsNeedsChanges() {
        String aiResponse = "{\"decision\": \"CHANGES_REQUESTED\", \"comments\": \"Missing error handling\"}";
        when(chatClientBuilder.build().prompt().user(anyString()).call().content()).thenReturn(aiResponse);

        Ticket ticket = buildTicket();
        AgentResult result = reviewerAgent.process(ticket);

        assertThat(result.needsChanges()).isTrue();
        assertThat(result.getMessage()).contains("Missing error handling");
        verify(agentLogService).log(eq(1L), eq(AgentType.REVIEWER), eq("CHANGES_REQUESTED"), anyString());
    }

    @Test
    void process_whenAiThrowsException_returnsFailure() {
        when(chatClientBuilder.build().prompt().user(anyString()).call().content())
                .thenThrow(new RuntimeException("AI service unavailable"));

        Ticket ticket = buildTicket();
        AgentResult result = reviewerAgent.process(ticket);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getMessage()).contains("AI service unavailable");
        verify(agentLogService).log(eq(1L), eq(AgentType.REVIEWER), eq("ERROR"), anyString());
    }

    @Test
    void process_whenResponseHasNoDecisionField_defaultsToChangesRequested() {
        String aiResponse = "{\"comments\": \"No decision field present\"}";
        when(chatClientBuilder.build().prompt().user(anyString()).call().content()).thenReturn(aiResponse);

        Ticket ticket = buildTicket();
        AgentResult result = reviewerAgent.process(ticket);

        assertThat(result.needsChanges()).isTrue();
    }

    @Test
    void process_setsAssignedAgentToReviewer() {
        String aiResponse = "{\"decision\": \"APPROVED\", \"comments\": \"LGTM\"}";
        when(chatClientBuilder.build().prompt().user(anyString()).call().content()).thenReturn(aiResponse);

        Ticket ticket = buildTicket();
        reviewerAgent.process(ticket);

        assertThat(ticket.getAssignedAgent()).isEqualTo(AgentType.REVIEWER);
    }

    @Test
    void process_broadcastsSseEvent() {
        String aiResponse = "{\"decision\": \"APPROVED\", \"comments\": \"OK\"}";
        when(chatClientBuilder.build().prompt().user(anyString()).call().content()).thenReturn(aiResponse);

        reviewerAgent.process(buildTicket());

        verify(sseService, atLeastOnce()).broadcast(any());
    }
}
