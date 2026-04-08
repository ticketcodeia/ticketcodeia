package TicketCodeIA.application.usecase.project;

import TicketCodeIA.application.command.CreateProjectCommand;
import TicketCodeIA.application.query.ProjectResult;
import TicketCodeIA.domain.model.project.Project;
import TicketCodeIA.domain.port.out.ProjectRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateProjectUseCase {

    private final ProjectRepositoryPort projectRepository;

    @Transactional
    public ProjectResult execute(CreateProjectCommand command) {
        Project project = Project.create(command.name(), command.description());
        Project saved = projectRepository.save(project);
        return ProjectResult.fromDomain(saved);
    }
}
