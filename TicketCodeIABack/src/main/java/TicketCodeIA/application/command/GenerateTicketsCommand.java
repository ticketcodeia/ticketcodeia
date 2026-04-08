package TicketCodeIA.application.command;

public record GenerateTicketsCommand(
        String requirements,
        Long projectId
) {}
