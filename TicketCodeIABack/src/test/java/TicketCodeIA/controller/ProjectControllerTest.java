package TicketCodeIA.controller;

import TicketCodeIA.dto.ProjectRequest;
import TicketCodeIA.dto.ProjectResponse;
import TicketCodeIA.service.ProjectService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProjectController.class)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProjectService projectService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private ProjectResponse sampleProject(Long id) {
        return ProjectResponse.builder()
                .id(id)
                .name("Project " + id)
                .description("Description " + id)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getAllProjects_returnsAllProjects() throws Exception {
        when(projectService.getAllProjects()).thenReturn(
                List.of(sampleProject(1L), sampleProject(2L)));

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    void getAllProjects_whenEmpty_returnsEmptyArray() throws Exception {
        when(projectService.getAllProjects()).thenReturn(List.of());

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getProjectById_whenFound_returnsProject() throws Exception {
        when(projectService.getProjectById(1L)).thenReturn(sampleProject(1L));

        mockMvc.perform(get("/api/projects/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Project 1"));
    }

    @Test
    void getProjectById_whenNotFound_returns500() throws Exception {
        when(projectService.getProjectById(99L))
                .thenThrow(new RuntimeException("Project not found: 99"));

        mockMvc.perform(get("/api/projects/99"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void createProject_returnsCreatedProject() throws Exception {
        ProjectRequest request = new ProjectRequest("New Project", "Some description");
        when(projectService.createProject(any(ProjectRequest.class))).thenReturn(sampleProject(3L));

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.name").value("Project 3"));
    }

    @Test
    void createProject_withMinimalData_returnsProject() throws Exception {
        ProjectRequest request = new ProjectRequest("Minimal", null);
        when(projectService.createProject(any())).thenReturn(
                ProjectResponse.builder().id(4L).name("Minimal").createdAt(LocalDateTime.now()).build());

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Minimal"));
    }
}
