package TicketCodeIA.application.query;

import TicketCodeIA.domain.enums.TicketStatus;

public record TicketQuery(
        Long projectId,
        TicketStatus status
) {}
