package TicketCodeIA.domain.port.out;

import TicketCodeIA.domain.model.project.Project;

import java.util.List;
import java.util.Optional;

public interface ProjectRepositoryPort {

    Project save(Project project);

    Optional<Project> findById(Long id);

    List<Project> findAll();
}
