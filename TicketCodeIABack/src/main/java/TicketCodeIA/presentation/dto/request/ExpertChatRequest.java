package TicketCodeIA.presentation.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpertChatRequest {
    private String sessionId;
    private String message;
    private Long projectId;
}
