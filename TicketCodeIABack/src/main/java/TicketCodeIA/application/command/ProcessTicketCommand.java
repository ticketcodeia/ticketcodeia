package TicketCodeIA.application.command;

public record ProcessTicketCommand(
        Long ticketId,
        boolean enableCodeReview,
        boolean enableTesting
) {}
