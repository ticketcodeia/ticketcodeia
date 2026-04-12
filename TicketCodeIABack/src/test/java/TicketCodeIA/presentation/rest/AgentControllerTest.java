package TicketCodeIA.presentation.rest;

import TicketCodeIA.infrastructure.agent.ExpertAgentAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AgentController.class)
class AgentControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private ExpertAgentAdapter expertAgentAdapter;

    @Test
    void expertChat_returns200() throws Exception {
        when(expertAgentAdapter.chat(any(), any(), any())).thenReturn("Hello! Tell me about your project.");

        mockMvc.perform(post("/api/agents/expert/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":\"s1\",\"message\":\"Hello\",\"projectId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hello! Tell me about your project."));
    }
}
