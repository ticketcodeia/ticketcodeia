package TicketCodeIA.infrastructure.persistence.mapper;

import TicketCodeIA.domain.enums.AgentType;
import TicketCodeIA.domain.enums.Priority;
import TicketCodeIA.domain.enums.TicketStatus;
import TicketCodeIA.domain.model.ticket.Ticket;
import TicketCodeIA.infrastructure.persistence.entity.ProjectJpaEntity;
import TicketCodeIA.infrastructure.persistence.entity.TicketJpaEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TicketPersistenceMapperTest {

    private final TicketPersistenceMapper mapper = new TicketPersistenceMapper();

    @Test
    void toDomain_mapsAllFields() {
        ProjectJpaEntity project = ProjectJpaEntity.builder().id(5L).name("MyProject").build();
        TicketJpaEntity entity = TicketJpaEntity.builder()
                .id(1L).title("Title").description("Desc")
                .status(TicketStatus.IN_PROGRESS).priority(Priority.HIGH)
                .assignedAgent(AgentType.DEVELOPER)
                .agentLogs(List.of("log1"))
                .project(project).branchName("feature/1")
                .enableCodeReview(true).enableTesting(false).retryCount(2)
                .build();

        Ticket domain = mapper.toDomain(entity);

        assertThat(domain.getId()).isEqualTo(1L);
        assertThat(domain.getTitle()).isEqualTo("Title");
        assertThat(domain.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(domain.getProjectId()).isEqualTo(5L);
        assertThat(domain.getProjectName()).isEqualTo("MyProject");
        assertThat(domain.isEnableCodeReview()).isTrue();
        assertThat(domain.getRetryCount()).isEqualTo(2);
    }

    @Test
    void toDomain_withNullProject_handlesGracefully() {
        TicketJpaEntity entity = TicketJpaEntity.builder()
                .id(1L).title("Title").description("Desc")
                .status(TicketStatus.TODO).priority(Priority.MEDIUM)
                .build();

        Ticket domain = mapper.toDomain(entity);

        assertThat(domain.getProjectId()).isNull();
        assertThat(domain.getProjectName()).isNull();
    }

    @Test
    void toJpaEntity_mapsAllFields() {
        Ticket domain = Ticket.create("Title", "Desc", Priority.HIGH, 5L, "MyProject");
        domain.setId(1L);
        domain.setBranchName("feature/1");

        TicketJpaEntity entity = mapper.toJpaEntity(domain);

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getTitle()).isEqualTo("Title");
        assertThat(entity.getProject()).isNotNull();
        assertThat(entity.getProject().getId()).isEqualTo(5L);
    }

    @Test
    void toJpaEntity_withNullProjectId_setsNullProject() {
        Ticket domain = Ticket.create("Title", "Desc", null, null, null);

        TicketJpaEntity entity = mapper.toJpaEntity(domain);

        assertThat(entity.getProject()).isNull();
    }

    @Test
    void roundTrip_preservesData() {
        Ticket original = Ticket.create("Title", "Desc", Priority.CRITICAL, 3L, "Proj");
        original.setId(42L);
        original.startDevelopment();

        TicketJpaEntity jpa = mapper.toJpaEntity(original);
        Ticket restored = mapper.toDomain(jpa);

        assertThat(restored.getId()).isEqualTo(42L);
        assertThat(restored.getTitle()).isEqualTo("Title");
        assertThat(restored.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(restored.getAssignedAgent()).isEqualTo(AgentType.DEVELOPER);
        assertThat(restored.getProjectId()).isEqualTo(3L);
    }
}
