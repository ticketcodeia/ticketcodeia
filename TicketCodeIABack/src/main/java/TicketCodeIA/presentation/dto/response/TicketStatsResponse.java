package TicketCodeIA.presentation.dto.response;

import TicketCodeIA.application.query.TicketStatsResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketStatsResponse {
    private long total;
    private long todo;
    private long inProgress;
    private long codeReview;
    private long testing;
    private long done;
    private long escalated;
    private long humanTodo;
    private long humanDev;
    private long humanReview;
    private long humanTesting;

    public static TicketStatsResponse fromResult(TicketStatsResult result) {
        return TicketStatsResponse.builder()
                .total(result.total())
                .todo(result.todo())
                .inProgress(result.inProgress())
                .codeReview(result.codeReview())
                .testing(result.testing())
                .done(result.done())
                .escalated(result.escalated())
                .humanTodo(result.humanTodo())
                .humanDev(result.humanDev())
                .humanReview(result.humanReview())
                .humanTesting(result.humanTesting())
                .build();
    }
}
