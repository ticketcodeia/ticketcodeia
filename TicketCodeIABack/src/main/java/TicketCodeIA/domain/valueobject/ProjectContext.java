package TicketCodeIA.domain.valueobject;

import TicketCodeIA.domain.model.ticket.Ticket;

import java.util.List;

/**
 * Full project context passed to agents so they understand the bigger picture.
 * Contains the project description and all tickets in the project with their current status.
 */
public record ProjectContext(
        String projectName,
        String projectDescription,
        List<TicketSummary> allTickets
) {
    /**
     * Lightweight summary of a ticket for context (no full description to save tokens).
     */
    public record TicketSummary(
            Long id,
            String title,
            String description,
            String status,
            String priority,
            String assignedAgent
    ) {
        public static TicketSummary fromTicket(Ticket ticket) {
            return new TicketSummary(
                    ticket.getId(),
                    ticket.getTitle(),
                    ticket.getDescription(),
                    ticket.getStatus().name(),
                    ticket.getPriority().name(),
                    ticket.getAssignedAgent() != null ? ticket.getAssignedAgent().name() : "NONE"
            );
        }
    }

    /**
     * Format the context as a readable string for the agent prompt.
     */
    public String toPromptString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== PROJECT CONTEXT ===\n");
        sb.append("Project: ").append(projectName).append("\n");
        if (projectDescription != null && !projectDescription.isBlank()) {
            sb.append("Description: ").append(projectDescription).append("\n");
        }
        sb.append("\n=== ALL TICKETS IN PROJECT ===\n");
        for (TicketSummary t : allTickets) {
            sb.append("- #").append(t.id())
                    .append(" [").append(t.status()).append("]")
                    .append(" [").append(t.priority()).append("]")
                    .append(" ").append(t.title()).append("\n");
        }
        sb.append("========================\n");
        return sb.toString();
    }
}
