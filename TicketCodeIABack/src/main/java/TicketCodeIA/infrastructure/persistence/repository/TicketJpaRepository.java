package TicketCodeIA.infrastructure.persistence.repository;

import TicketCodeIA.domain.enums.TicketStatus;
import TicketCodeIA.infrastructure.persistence.entity.TicketJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketJpaRepository extends JpaRepository<TicketJpaEntity, Long> {

    List<TicketJpaEntity> findByStatus(TicketStatus status);

    List<TicketJpaEntity> findByProjectId(Long projectId);

    List<TicketJpaEntity> findByProjectIdAndStatus(Long projectId, TicketStatus status);

    long countByStatus(TicketStatus status);

    List<TicketJpaEntity> findAllByOrderByCreatedAtDesc();

    List<TicketJpaEntity> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    @Query("SELECT t FROM TicketJpaEntity t LEFT JOIN FETCH t.project WHERE t.id = :id")
    Optional<TicketJpaEntity> findByIdWithProject(@Param("id") Long id);
}
