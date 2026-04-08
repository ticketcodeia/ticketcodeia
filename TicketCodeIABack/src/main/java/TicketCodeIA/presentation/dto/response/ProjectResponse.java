package TicketCodeIA.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import TicketCodeIA.application.query.ProjectResult;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;

    public static ProjectResponse fromResult(ProjectResult result) {
        return ProjectResponse.builder()
                .id(result.id())
                .name(result.name())
                .description(result.description())
                .createdAt(result.createdAt())
                .build();
    }
}
