package TicketCodeIA.presentation.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessTicketRequest {
    private boolean enableCodeReview = true;
    private boolean enableTesting = true;
}
