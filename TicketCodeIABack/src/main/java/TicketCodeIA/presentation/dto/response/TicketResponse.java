package TicketCodeIA.presentation.dto.response;

import TicketCodeIA.application.query.TicketResult;
import TicketCodeIA.domain.enums.AgentType;
import TicketCodeIA.domain.enums.Priority;
import TicketCodeIA.domain.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {
    private Long id;
    private String title;
    private String description;
    private TicketStatus status;
    private Priority priority;
    private AgentType assignedAgent;
    private List<String> agentLogs;
    private Long projectId;
    private String projectName;
    private String branchName;
    private boolean enableCodeReview;
    private boolean enableTesting;
    private int retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TicketResponse fromResult(TicketResult result) {
        return TicketResponse.builder()
                .id(result.id())
                .title(result.title())
                .description(result.description())
                .status(result.status())
                .priority(result.priority())
                .assignedAgent(result.assignedAgent())
                .agentLogs(result.agentLogs())
                .projectId(result.projectId())
                .projectName(result.projectName())
                .branchName(result.branchName())
                .enableCodeReview(result.enableCodeReview())
                .enableTesting(result.enableTesting())
                .retryCount(result.retryCount())
                .createdAt(result.createdAt())
                .updatedAt(result.updatedAt())
                .build();
    }
}
