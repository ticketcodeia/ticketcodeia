package TicketCodeIA.infrastructure.persistence.mapper;

import TicketCodeIA.domain.model.project.Project;
import TicketCodeIA.infrastructure.persistence.entity.ProjectJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ProjectPersistenceMapper {

    public Project toDomain(ProjectJpaEntity entity) {
        return new Project(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCreatedAt()
        );
    }

    public ProjectJpaEntity toJpaEntity(Project domain) {
        return ProjectJpaEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .description(domain.getDescription())
                .build();
    }
}
