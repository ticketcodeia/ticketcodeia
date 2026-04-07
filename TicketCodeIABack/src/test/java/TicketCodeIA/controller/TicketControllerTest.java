package TicketCodeIA.controller;

import TicketCodeIA.agent.AgentOrchestrator;
import TicketCodeIA.dto.TicketRequest;
import TicketCodeIA.dto.TicketResponse;
import TicketCodeIA.dto.TicketStats;
import TicketCodeIA.entity.AgentLog;
import TicketCodeIA.enums.AgentType;
import TicketCodeIA.enums.Priority;
import TicketCodeIA.enums.TicketStatus;
import TicketCodeIA.service.AgentLogService;
import TicketCodeIA.service.TicketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TicketController.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private TicketService ticketService;
    @MockBean private AgentOrchestrator agentOrchestrator;
    @MockBean private AgentLogService agentLogService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private TicketResponse sampleTicketResponse(Long id) {
        return TicketResponse.builder()
                .id(id)
                .title("Test Ticket")
                .description("Description")
                .status(TicketStatus.TODO)
                .priority(Priority.MEDIUM)
                .enableCodeReview(true)
                .enableTesting(true)
                .agentLogs(List.of())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createTicket_returnsCreatedTicket() throws Exception {
        TicketRequest request = new TicketRequest();
        request.setTitle("New Feature");
        request.setDescription("Implement it");

        when(ticketService.createTicket(any(TicketRequest.class))).thenReturn(sampleTicketResponse(1L));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Ticket"))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    void getAllTickets_withNoParams_returnsAll() throws Exception {
        when(ticketService.getAllTickets(null, null))
                .thenReturn(List.of(sampleTicketResponse(1L), sampleTicketResponse(2L)));

        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getAllTickets_withStatusParam_filtersByStatus() throws Exception {
        when(ticketService.getAllTickets(null, TicketStatus.ESCALATED))
                .thenReturn(List.of(sampleTicketResponse(1L)));

        mockMvc.perform(get("/api/tickets").param("status", "ESCALATED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getAllTickets_withProjectIdParam_filtersByProject() throws Exception {
        when(ticketService.getAllTickets(5L, null)).thenReturn(List.of(sampleTicketResponse(1L)));

        mockMvc.perform(get("/api/tickets").param("projectId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getTicketById_whenFound_returnsTicket() throws Exception {
        when(ticketService.getTicketById(1L)).thenReturn(sampleTicketResponse(1L));

        mockMvc.perform(get("/api/tickets/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateTicket_returnsUpdatedTicket() throws Exception {
        TicketRequest request = new TicketRequest();
        request.setTitle("Updated");
        request.setStatus(TicketStatus.IN_PROGRESS);

        TicketResponse updated = sampleTicketResponse(1L);
        when(ticketService.updateTicket(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(put("/api/tickets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void processTicket_returnsAccepted() throws Exception {
        when(ticketService.getTicketById(1L)).thenReturn(sampleTicketResponse(1L));
        doNothing().when(ticketService).saveAgentFlags(anyLong(), anyBoolean(), anyBoolean());
        doNothing().when(agentOrchestrator).processTicketAsync(anyLong(), anyBoolean(), anyBoolean());

        mockMvc.perform(post("/api/tickets/1/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enableCodeReview\":true,\"enableTesting\":true}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getStats_returnsStats() throws Exception {
        TicketStats stats = TicketStats.builder()
                .total(10).todo(3).inProgress(2).codeReview(1).testing(1).done(2).escalated(1).build();
        when(ticketService.getStats()).thenReturn(stats);

        mockMvc.perform(get("/api/tickets/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.todo").value(3))
                .andExpect(jsonPath("$.done").value(2));
    }

    @Test
    void getTicketLogs_returnsLogs() throws Exception {
        List<AgentLog> logs = List.of(
                AgentLog.builder().id(1L).ticketId(1L)
                        .agentType(AgentType.DEVELOPER).action("STARTED").message("Started").build()
        );
        when(agentLogService.getLogsByTicketId(1L)).thenReturn(logs);

        mockMvc.perform(get("/api/tickets/1/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].action").value("STARTED"));
    }
}
