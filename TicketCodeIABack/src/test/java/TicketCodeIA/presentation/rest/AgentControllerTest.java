package TicketCodeIA.presentation.rest;

import TicketCodeIA.application.query.TicketResult;
import TicketCodeIA.application.usecase.agent.GenerateTicketsFromRequirementsUseCase;
import TicketCodeIA.domain.enums.Priority;
import TicketCodeIA.domain.enums.TicketStatus;
import TicketCodeIA.infrastructure.agent.ExpertAgentAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AgentController.class)
class AgentControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private GenerateTicketsFromRequirementsUseCase generateTicketsUseCase;
    @MockBean private ExpertAgentAdapter expertAgentAdapter;

    @Test
    void generateTickets_returns200() throws Exception {
        var ticketResult = new TicketResult(1L, "Generated", "Desc", TicketStatus.TODO, Priority.MEDIUM,
                null, List.of(), null, null, null, false, false, 0,
                LocalDateTime.now(), LocalDateTime.now());
        when(generateTicketsUseCase.execute(any(), any())).thenReturn(List.of(ticketResult));

        mockMvc.perform(post("/api/agents/generate-tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requirements\":\"Build a login\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Generated"));
    }

    @Test
    void generateTickets_whenFails_returns500() throws Exception {
        when(generateTicketsUseCase.execute(any(), any()))
                .thenThrow(new RuntimeException("AI service unavailable"));

        mockMvc.perform(post("/api/agents/generate-tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requirements\":\"test\"}"))
                .andExpect(status().is5xxServerError());
    }
}
