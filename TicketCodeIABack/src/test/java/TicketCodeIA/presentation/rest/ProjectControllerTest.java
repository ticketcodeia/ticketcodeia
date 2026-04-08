package TicketCodeIA.presentation.rest;

import TicketCodeIA.application.query.ProjectResult;
import TicketCodeIA.application.usecase.project.CreateProjectUseCase;
import TicketCodeIA.application.usecase.project.GetProjectUseCase;
import TicketCodeIA.domain.exception.ProjectNotFoundException;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProjectController.class)
class ProjectControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private CreateProjectUseCase createProjectUseCase;
    @MockBean private GetProjectUseCase getProjectUseCase;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void getAllProjects_returnsProjects() throws Exception {
        when(getProjectUseCase.getAll()).thenReturn(List.of(
                new ProjectResult(1L, "P1", "d1", LocalDateTime.now()),
                new ProjectResult(2L, "P2", "d2", LocalDateTime.now())));

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getProjectById_returnsProject() throws Exception {
        when(getProjectUseCase.getById(1L)).thenReturn(
                new ProjectResult(1L, "P1", "d1", LocalDateTime.now()));

        mockMvc.perform(get("/api/projects/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("P1"));
    }

    @Test
    void getProjectById_notFound_returns404() throws Exception {
        when(getProjectUseCase.getById(99L)).thenThrow(new ProjectNotFoundException(99L));

        mockMvc.perform(get("/api/projects/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createProject_returnsCreated() throws Exception {
        when(createProjectUseCase.execute(any())).thenReturn(
                new ProjectResult(3L, "New", "desc", LocalDateTime.now()));

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New\",\"description\":\"desc\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3));
    }
}
