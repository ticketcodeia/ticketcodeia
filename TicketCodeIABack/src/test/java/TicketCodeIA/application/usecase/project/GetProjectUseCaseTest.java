package TicketCodeIA.application.usecase.project;

import TicketCodeIA.application.query.ProjectResult;
import TicketCodeIA.domain.exception.ProjectNotFoundException;
import TicketCodeIA.domain.model.project.Project;
import TicketCodeIA.domain.port.out.ProjectRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetProjectUseCaseTest {

    @Mock private ProjectRepositoryPort projectRepository;
    @InjectMocks private GetProjectUseCase useCase;

    @Test
    void getAll_returnsProjects() {
        when(projectRepository.findAll()).thenReturn(
                List.of(new Project(1L, "P1", "d1", null), new Project(2L, "P2", "d2", null)));
        List<ProjectResult> results = useCase.getAll();
        assertThat(results).hasSize(2);
    }

    @Test
    void getById_returnsProject() {
        when(projectRepository.findById(1L)).thenReturn(
                Optional.of(new Project(1L, "P1", "d1", null)));
        ProjectResult result = useCase.getById(1L);
        assertThat(result.name()).isEqualTo("P1");
    }

    @Test
    void getById_notFound_throws() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.getById(99L)).isInstanceOf(ProjectNotFoundException.class);
    }
}
