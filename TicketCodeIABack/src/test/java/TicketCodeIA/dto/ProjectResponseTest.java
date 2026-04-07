package TicketCodeIA.dto;

import TicketCodeIA.entity.Project;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectResponseTest {

    @Test
    void fromEntity_mapsAllFields() {
        LocalDateTime now = LocalDateTime.of(2024, 1, 15, 10, 30);
        Project project = Project.builder()
                .id(5L)
                .name("Backend API")
                .description("REST API project")
                .createdAt(now)
                .build();

        ProjectResponse response = ProjectResponse.fromEntity(project);

        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getName()).isEqualTo("Backend API");
        assertThat(response.getDescription()).isEqualTo("REST API project");
        assertThat(response.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void fromEntity_withNullDescription_mapsNull() {
        Project project = Project.builder()
                .id(1L)
                .name("No Desc")
                .description(null)
                .build();

        ProjectResponse response = ProjectResponse.fromEntity(project);

        assertThat(response.getDescription()).isNull();
    }

    @Test
    void fromEntity_withNullCreatedAt_mapsNull() {
        Project project = Project.builder()
                .id(1L)
                .name("Project")
                .createdAt(null)
                .build();

        ProjectResponse response = ProjectResponse.fromEntity(project);

        assertThat(response.getCreatedAt()).isNull();
    }
}
