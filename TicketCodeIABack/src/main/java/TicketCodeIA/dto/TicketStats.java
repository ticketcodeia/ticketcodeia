package TicketCodeIA.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketStats {
    private long total;
    private long todo;
    private long inProgress;
    private long codeReview;
    private long testing;
    private long done;
    private long escalated;
}
