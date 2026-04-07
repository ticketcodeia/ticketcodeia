package TicketCodeIA.service;

import TicketCodeIA.dto.ProjectRequest;
import TicketCodeIA.dto.ProjectResponse;
import TicketCodeIA.entity.Project;
import TicketCodeIA.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void getAllProjects_returnsAllProjects() {
        List<Project> projects = List.of(
                Project.builder().id(1L).name("P1").description("Desc1").createdAt(LocalDateTime.now()).build(),
                Project.builder().id(2L).name("P2").description("Desc2").createdAt(LocalDateTime.now()).build()
        );
        when(projectRepository.findAll()).thenReturn(projects);

        List<ProjectResponse> result = projectService.getAllProjects();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("P1");
        assertThat(result.get(1).getName()).isEqualTo("P2");
    }

    @Test
    void getAllProjects_whenEmpty_returnsEmptyList() {
        when(projectRepository.findAll()).thenReturn(List.of());

        List<ProjectResponse> result = projectService.getAllProjects();

        assertThat(result).isEmpty();
    }

    @Test
    void getProjectById_whenFound_returnsResponse() {
        Project project = Project.builder()
                .id(1L).name("Test").description("Desc").createdAt(LocalDateTime.now()).build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        ProjectResponse result = projectService.getProjectById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test");
    }

    @Test
    void getProjectById_whenNotFound_throwsException() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProjectById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Project not found");
    }

    @Test
    void createProject_savesAndReturnsProject() {
        ProjectRequest request = new ProjectRequest("New Project", "New Description");
        Project saved = Project.builder()
                .id(1L).name("New Project").description("New Description").createdAt(LocalDateTime.now()).build();
        when(projectRepository.save(any(Project.class))).thenReturn(saved);

        ProjectResponse result = projectService.createProject(request);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("New Project");
        assertThat(result.getDescription()).isEqualTo("New Description");

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("New Project");
    }

    @Test
    void getEntityById_whenFound_returnsEntity() {
        Project project = Project.builder().id(1L).name("Direct").build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        Project result = projectService.getEntityById(1L);

        assertThat(result).isEqualTo(project);
    }

    @Test
    void getEntityById_whenNotFound_throwsException() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getEntityById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Project not found");
    }
}
