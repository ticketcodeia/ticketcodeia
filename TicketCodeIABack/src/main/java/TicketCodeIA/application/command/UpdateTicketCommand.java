package TicketCodeIA.application.command;

import TicketCodeIA.domain.enums.Priority;
import TicketCodeIA.domain.enums.TicketStatus;

public record UpdateTicketCommand(
        Long id,
        String title,
        String description,
        Priority priority,
        TicketStatus status
) {}
