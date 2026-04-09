package TicketCodeIA.application.query;

public record TicketStatsResult(
        long total,
        long todo,
        long inProgress,
        long codeReview,
        long testing,
        long done,
        long escalated,
        long humanTodo,
        long humanDev,
        long humanReview,
        long humanTesting
) {}
