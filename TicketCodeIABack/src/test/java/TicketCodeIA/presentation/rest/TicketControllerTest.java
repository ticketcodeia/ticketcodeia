package TicketCodeIA.presentation.rest;

import TicketCodeIA.application.query.AgentLogResult;
import TicketCodeIA.application.query.TicketResult;
import TicketCodeIA.application.query.TicketStatsResult;
import TicketCodeIA.application.usecase.agentlog.GetAgentLogsUseCase;
import TicketCodeIA.application.usecase.ticket.*;
import TicketCodeIA.domain.enums.AgentType;
import TicketCodeIA.domain.enums.Priority;
import TicketCodeIA.domain.enums.TicketStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TicketController.class)
class TicketControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private CreateTicketUseCase createTicketUseCase;
    @MockBean private GetTicketUseCase getTicketUseCase;
    @MockBean private UpdateTicketUseCase updateTicketUseCase;
    @MockBean private GetTicketStatsUseCase getTicketStatsUseCase;
    @MockBean private ProcessTicketUseCase processTicketUseCase;
    @MockBean private GetAgentLogsUseCase getAgentLogsUseCase;
    @MockBean private MoveTicketOnHumanBoardUseCase moveTicketOnHumanBoardUseCase;

    private TicketResult sampleResult(Long id) {
        return new TicketResult(id, "Title " + id, "Desc", TicketStatus.TODO, Priority.MEDIUM,
                null, List.of(), null, null, null, false, false, 0,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void createTicket_returns200() throws Exception {
        when(createTicketUseCase.execute(any())).thenReturn(sampleResult(1L));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test\",\"description\":\"Desc\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getAllTickets_returns200() throws Exception {
        when(getTicketUseCase.getAll(any())).thenReturn(List.of(sampleResult(1L), sampleResult(2L)));

        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getTicketById_returns200() throws Exception {
        when(getTicketUseCase.getById(1L)).thenReturn(sampleResult(1L));

        mockMvc.perform(get("/api/tickets/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Title 1"));
    }

    @Test
    void updateTicket_returns200() throws Exception {
        when(updateTicketUseCase.execute(any())).thenReturn(sampleResult(1L));

        mockMvc.perform(put("/api/tickets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void processTicket_returns202() throws Exception {
        when(getTicketUseCase.getById(1L)).thenReturn(sampleResult(1L));
        doNothing().when(processTicketUseCase).saveAgentFlags(anyLong(), anyBoolean(), anyBoolean());
        doNothing().when(processTicketUseCase).executeAsync(anyLong(), anyBoolean(), anyBoolean());

        mockMvc.perform(post("/api/tickets/1/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enableCodeReview\":true,\"enableTesting\":true}"))
                .andExpect(status().isAccepted());
    }

    @Test
    void getStats_returns200() throws Exception {
        when(getTicketStatsUseCase.execute()).thenReturn(
                new TicketStatsResult(10, 3, 2, 1, 1, 2, 1, 0, 0, 0, 0));

        mockMvc.perform(get("/api/tickets/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(10));
    }

    @Test
    void getTicketLogs_returns200() throws Exception {
        when(getAgentLogsUseCase.getByTicketId(1L)).thenReturn(List.of(
                new AgentLogResult(1L, 1L, AgentType.DEVELOPER, "STARTED", "msg", LocalDateTime.now())));

        mockMvc.perform(get("/api/tickets/1/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
