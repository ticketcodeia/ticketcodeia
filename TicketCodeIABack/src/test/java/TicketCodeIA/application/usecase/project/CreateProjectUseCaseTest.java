package TicketCodeIA.application.usecase.project;

import TicketCodeIA.application.command.CreateProjectCommand;
import TicketCodeIA.application.query.ProjectResult;
import TicketCodeIA.domain.model.project.Project;
import TicketCodeIA.domain.port.out.ProjectRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateProjectUseCaseTest {

    @Mock private ProjectRepositoryPort projectRepository;
    @InjectMocks private CreateProjectUseCase useCase;

    @Test
    void execute_createsProject() {
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        ProjectResult result = useCase.execute(new CreateProjectCommand("MyProject", "desc"));

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("MyProject");
    }
}
