package TicketCodeIA.application.query;

import TicketCodeIA.domain.enums.AgentType;
import TicketCodeIA.domain.enums.Priority;
import TicketCodeIA.domain.enums.TicketStatus;
import TicketCodeIA.domain.model.ticket.Ticket;

import java.time.LocalDateTime;
import java.util.List;

public record TicketResult(
        Long id,
        String title,
        String description,
        TicketStatus status,
        Priority priority,
        AgentType assignedAgent,
        List<String> agentLogs,
        Long projectId,
        String projectName,
        String branchName,
        boolean enableCodeReview,
        boolean enableTesting,
        int retryCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TicketResult fromDomain(Ticket ticket) {
        return new TicketResult(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getAssignedAgent(),
                ticket.getAgentLogs(),
                ticket.getProjectId(),
                ticket.getProjectName(),
                ticket.getBranchName(),
                ticket.isEnableCodeReview(),
                ticket.isEnableTesting(),
                ticket.getRetryCount(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}
