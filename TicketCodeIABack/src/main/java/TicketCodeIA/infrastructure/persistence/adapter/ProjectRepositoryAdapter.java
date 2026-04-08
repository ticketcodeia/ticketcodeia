package TicketCodeIA.infrastructure.persistence.adapter;

import TicketCodeIA.domain.model.project.Project;
import TicketCodeIA.domain.port.out.ProjectRepositoryPort;
import TicketCodeIA.infrastructure.persistence.mapper.ProjectPersistenceMapper;
import TicketCodeIA.infrastructure.persistence.repository.ProjectJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProjectRepositoryAdapter implements ProjectRepositoryPort {

    private final ProjectJpaRepository jpaRepository;
    private final ProjectPersistenceMapper mapper;

    @Override
    public Project save(Project project) {
        var entity = mapper.toJpaEntity(project);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Project> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Project> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }
}
