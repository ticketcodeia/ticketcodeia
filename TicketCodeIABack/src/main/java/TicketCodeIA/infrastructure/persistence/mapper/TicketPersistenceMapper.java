package TicketCodeIA.infrastructure.persistence.mapper;

import TicketCodeIA.domain.model.ticket.Ticket;
import TicketCodeIA.infrastructure.persistence.entity.ProjectJpaEntity;
import TicketCodeIA.infrastructure.persistence.entity.TicketJpaEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class TicketPersistenceMapper {

    public Ticket toDomain(TicketJpaEntity entity) {
        Long projectId = null;
        String projectName = null;
        if (entity.getProject() != null) {
            projectId = entity.getProject().getId();
            projectName = entity.getProject().getName();
        }

        return new Ticket(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getPriority(),
                entity.getAssignedAgent(),
                entity.getAgentLogs() != null ? new ArrayList<>(entity.getAgentLogs()) : new ArrayList<>(),
                projectId,
                projectName,
                entity.getBranchName(),
                entity.isEnableCodeReview(),
                entity.isEnableTesting(),
                entity.getRetryCount(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public TicketJpaEntity toJpaEntity(Ticket domain) {
        TicketJpaEntity entity = TicketJpaEntity.builder()
                .id(domain.getId())
                .title(domain.getTitle())
                .description(domain.getDescription())
                .status(domain.getStatus())
                .priority(domain.getPriority())
                .assignedAgent(domain.getAssignedAgent())
                .agentLogs(new ArrayList<>(domain.getAgentLogs()))
                .branchName(domain.getBranchName())
                .enableCodeReview(domain.isEnableCodeReview())
                .enableTesting(domain.isEnableTesting())
                .retryCount(domain.getRetryCount())
                .build();

        if (domain.getProjectId() != null) {
            ProjectJpaEntity project = new ProjectJpaEntity();
            project.setId(domain.getProjectId());
            entity.setProject(project);
        }

        return entity;
    }

    public void updateJpaEntity(TicketJpaEntity existing, Ticket domain) {
        existing.setTitle(domain.getTitle());
        existing.setDescription(domain.getDescription());
        existing.setStatus(domain.getStatus());
        existing.setPriority(domain.getPriority());
        existing.setAssignedAgent(domain.getAssignedAgent());
        existing.setAgentLogs(new ArrayList<>(domain.getAgentLogs()));
        existing.setBranchName(domain.getBranchName());
        existing.setEnableCodeReview(domain.isEnableCodeReview());
        existing.setEnableTesting(domain.isEnableTesting());
        existing.setRetryCount(domain.getRetryCount());

        if (domain.getProjectId() != null) {
            ProjectJpaEntity project = new ProjectJpaEntity();
            project.setId(domain.getProjectId());
            existing.setProject(project);
        } else {
            existing.setProject(null);
        }
    }
}
