package TicketCodeIA.application.query;

import TicketCodeIA.domain.model.project.Project;

import java.time.LocalDateTime;

public record ProjectResult(
        Long id,
        String name,
        String description,
        LocalDateTime createdAt
) {
    public static ProjectResult fromDomain(Project project) {
        return new ProjectResult(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getCreatedAt()
        );
    }
}
