package TicketCodeIA.presentation.rest;

import TicketCodeIA.application.query.AgentLogResult;
import TicketCodeIA.application.usecase.agentlog.GetAgentLogsUseCase;
import TicketCodeIA.domain.enums.AgentType;
import TicketCodeIA.infrastructure.sse.SseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SseController.class)
class SseControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private SseService sseService;
    @MockBean private GetAgentLogsUseCase getAgentLogsUseCase;

    @Test
    void getRecentActivity_returnsLogs() throws Exception {
        when(getAgentLogsUseCase.getRecentLogs()).thenReturn(List.of(
                new AgentLogResult(1L, 1L, AgentType.DEVELOPER, "STARTED", "Working", LocalDateTime.now())));

        mockMvc.perform(get("/api/sse/recent-activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].action").value("STARTED"));
    }

    @Test
    void getRecentActivity_empty_returnsEmptyArray() throws Exception {
        when(getAgentLogsUseCase.getRecentLogs()).thenReturn(List.of());

        mockMvc.perform(get("/api/sse/recent-activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
