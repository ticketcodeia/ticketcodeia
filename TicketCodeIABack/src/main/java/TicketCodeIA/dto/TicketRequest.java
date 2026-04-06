package TicketCodeIA.dto;

import TicketCodeIA.enums.Priority;
import TicketCodeIA.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketRequest {
    private String title;
    private String description;
    private Priority priority;
    private TicketStatus status;
}
