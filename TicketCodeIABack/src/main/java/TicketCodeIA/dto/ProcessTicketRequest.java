package TicketCodeIA.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessTicketRequest {
    private boolean enableCodeReview = true;
    private boolean enableTesting = true;
}
