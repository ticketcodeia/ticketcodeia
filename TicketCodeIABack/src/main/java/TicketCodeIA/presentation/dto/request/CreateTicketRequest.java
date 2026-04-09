package TicketCodeIA.presentation.dto.request;

import TicketCodeIA.domain.enums.Priority;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTicketRequest {
    private String title;
    private String description;
    private Priority priority;
    private Long projectId;
}
