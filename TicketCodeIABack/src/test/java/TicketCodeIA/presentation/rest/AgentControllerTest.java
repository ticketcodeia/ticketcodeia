package TicketCodeIA.presentation.rest;

import TicketCodeIA.application.query.ChatMessageResult;
import TicketCodeIA.application.usecase.chat.ClearChatSessionUseCase;
import TicketCodeIA.application.usecase.chat.GetChatHistoryUseCase;
import TicketCodeIA.application.usecase.chat.SendChatMessageUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AgentController.class)
class AgentControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private SendChatMessageUseCase sendChatMessageUseCase;
    @MockBean private GetChatHistoryUseCase getChatHistoryUseCase;
    @MockBean private ClearChatSessionUseCase clearChatSessionUseCase;

    @Test
    void expertChat_returns200() throws Exception {
        var result = new ChatMessageResult(1L, "s1", 1L, "assistant",
                "Hello! Tell me about your project.", LocalDateTime.now());
        when(sendChatMessageUseCase.execute(any(), any(), any())).thenReturn(result);

        mockMvc.perform(post("/api/agents/expert/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":\"s1\",\"message\":\"Hello\",\"projectId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hello! Tell me about your project."));
    }

    @Test
    void getChatHistory_returns200() throws Exception {
        var msg1 = new ChatMessageResult(1L, "s1", 1L, "user", "Hello", LocalDateTime.now());
        var msg2 = new ChatMessageResult(2L, "s1", 1L, "assistant", "Hi!", LocalDateTime.now());
        when(getChatHistoryUseCase.bySession("s1")).thenReturn(List.of(msg1, msg2));

        mockMvc.perform(get("/api/agents/expert/session/s1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].role").value("user"))
                .andExpect(jsonPath("$[1].role").value("assistant"));
    }

    @Test
    void clearSession_returns204() throws Exception {
        mockMvc.perform(delete("/api/agents/expert/session/s1"))
                .andExpect(status().isNoContent());

        verify(clearChatSessionUseCase).execute("s1");
    }
}
