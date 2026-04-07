package TicketCodeIA.controller;

import TicketCodeIA.entity.AgentLog;
import TicketCodeIA.enums.AgentType;
import TicketCodeIA.service.AgentLogService;
import TicketCodeIA.service.SseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SseController.class)
class SseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SseService sseService;

    @MockBean
    private AgentLogService agentLogService;

    @Test
    void getRecentActivity_returnsLogs() throws Exception {
        List<AgentLog> logs = List.of(
                AgentLog.builder().id(1L).ticketId(1L)
                        .agentType(AgentType.DEVELOPER).action("STARTED").message("Working").build(),
                AgentLog.builder().id(2L).ticketId(2L)
                        .agentType(AgentType.TESTER).action("COMPLETED").message("Done").build()
        );
        when(agentLogService.getRecentLogs()).thenReturn(logs);

        mockMvc.perform(get("/api/sse/recent-activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].action").value("STARTED"))
                .andExpect(jsonPath("$[1].action").value("COMPLETED"));
    }

    @Test
    void getRecentActivity_whenNoLogs_returnsEmptyArray() throws Exception {
        when(agentLogService.getRecentLogs()).thenReturn(List.of());

        mockMvc.perform(get("/api/sse/recent-activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getRecentActivity_callsAgentLogService() throws Exception {
        when(agentLogService.getRecentLogs()).thenReturn(List.of());

        mockMvc.perform(get("/api/sse/recent-activity"));

        verify(agentLogService).getRecentLogs();
    }
}
