package TicketCodeIA.application.command;

import TicketCodeIA.domain.enums.Priority;

public record CreateTicketCommand(
        String title,
        String description,
        Priority priority,
        Long projectId
) {}
