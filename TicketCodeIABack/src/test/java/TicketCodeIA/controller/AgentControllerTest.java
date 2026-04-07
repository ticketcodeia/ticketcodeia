package TicketCodeIA.controller;

import TicketCodeIA.agent.POAgent;
import TicketCodeIA.dto.RequirementsRequest;
import TicketCodeIA.dto.TicketResponse;
import TicketCodeIA.enums.Priority;
import TicketCodeIA.enums.TicketStatus;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AgentController.class)
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private POAgent poAgent;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private TicketResponse sampleTicket(Long id) {
        return TicketResponse.builder()
                .id(id)
                .title("Generated Ticket " + id)
                .description("Auto-generated")
                .status(TicketStatus.TODO)
                .priority(Priority.MEDIUM)
                .agentLogs(List.of())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void generateTickets_withRequirements_returnsTickets() throws Exception {
        RequirementsRequest request = new RequirementsRequest("Build a login system", null);
        List<TicketResponse> generatedTickets = List.of(sampleTicket(1L), sampleTicket(2L), sampleTicket(3L));

        when(poAgent.generateTicketsFromRequirements(eq("Build a login system"), eq(null)))
                .thenReturn(generatedTickets);

        mockMvc.perform(post("/api/agents/generate-tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].title").value("Generated Ticket 1"))
                .andExpect(jsonPath("$[1].status").value("TODO"));
    }

    @Test
    void generateTickets_withProjectId_passesProjectIdToAgent() throws Exception {
        RequirementsRequest request = new RequirementsRequest("Build dashboard", 5L);
        when(poAgent.generateTicketsFromRequirements(eq("Build dashboard"), eq(5L)))
                .thenReturn(List.of(sampleTicket(10L)));

        mockMvc.perform(post("/api/agents/generate-tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    void generateTickets_whenAgentFails_returns500() throws Exception {
        RequirementsRequest request = new RequirementsRequest("requirements", null);
        when(poAgent.generateTicketsFromRequirements(any(), any()))
                .thenThrow(new RuntimeException("AI service unavailable"));

        mockMvc.perform(post("/api/agents/generate-tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void generateTickets_withEmptyResult_returnsEmptyArray() throws Exception {
        RequirementsRequest request = new RequirementsRequest("minimal", null);
        when(poAgent.generateTicketsFromRequirements(any(), any())).thenReturn(List.of());

        mockMvc.perform(post("/api/agents/generate-tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
