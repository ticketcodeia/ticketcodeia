package TicketCodeIA.presentation.rest;

import TicketCodeIA.application.command.CreateProjectCommand;
import TicketCodeIA.application.usecase.project.CreateProjectUseCase;
import TicketCodeIA.application.usecase.project.GetProjectUseCase;
import TicketCodeIA.presentation.dto.response.ProjectResponse;
import TicketCodeIA.presentation.dto.request.CreateProjectRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final CreateProjectUseCase createProjectUseCase;
    private final GetProjectUseCase getProjectUseCase;

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {
        List<ProjectResponse> projects = getProjectUseCase.getAll().stream()
                .map(ProjectResponse::fromResult).toList();
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable Long id) {
        return ResponseEntity.ok(ProjectResponse.fromResult(getProjectUseCase.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@RequestBody CreateProjectRequest request) {
        var command = new CreateProjectCommand(request.getName(), request.getDescription());
        return ResponseEntity.ok(ProjectResponse.fromResult(createProjectUseCase.execute(command)));
    }
}
