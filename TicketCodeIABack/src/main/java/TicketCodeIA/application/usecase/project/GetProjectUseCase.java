package TicketCodeIA.application.usecase.project;

import TicketCodeIA.application.query.ProjectResult;
import TicketCodeIA.domain.exception.ProjectNotFoundException;
import TicketCodeIA.domain.port.out.ProjectRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetProjectUseCase {

    private final ProjectRepositoryPort projectRepository;

    @Transactional(readOnly = true)
    public List<ProjectResult> getAll() {
        return projectRepository.findAll().stream()
                .map(ProjectResult::fromDomain)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResult getById(Long id) {
        return projectRepository.findById(id)
                .map(ProjectResult::fromDomain)
                .orElseThrow(() -> new ProjectNotFoundException(id));
    }
}
