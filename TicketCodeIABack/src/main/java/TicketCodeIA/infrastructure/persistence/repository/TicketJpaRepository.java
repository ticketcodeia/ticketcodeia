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

    @Query("SELECT t FROM TicketJpaEntity t LEFT JOIN FETCH t.project")
    List<TicketJpaEntity> findAllWithProject();

    @Query("SELECT t FROM TicketJpaEntity t LEFT JOIN FETCH t.project WHERE t.status = :status")
    List<TicketJpaEntity> findByStatus(@Param("status") TicketStatus status);

    @Query("SELECT t FROM TicketJpaEntity t LEFT JOIN FETCH t.project WHERE t.project.id = :projectId")
    List<TicketJpaEntity> findByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT t FROM TicketJpaEntity t LEFT JOIN FETCH t.project WHERE t.project.id = :projectId AND t.status = :status")
    List<TicketJpaEntity> findByProjectIdAndStatus(@Param("projectId") Long projectId, @Param("status") TicketStatus status);

    long countByStatus(TicketStatus status);

    @Query("SELECT COUNT(t) FROM TicketJpaEntity t WHERE t.project.id = :projectId AND t.status IN :statuses")
    long countByProjectIdAndStatusIn(@Param("projectId") Long projectId, @Param("statuses") List<TicketStatus> statuses);

    @Query("SELECT t FROM TicketJpaEntity t LEFT JOIN FETCH t.project ORDER BY t.createdAt DESC")
    List<TicketJpaEntity> findAllByOrderByCreatedAtDesc();

    @Query("SELECT t FROM TicketJpaEntity t LEFT JOIN FETCH t.project WHERE t.project.id = :projectId ORDER BY t.createdAt DESC")
    List<TicketJpaEntity> findByProjectIdOrderByCreatedAtDesc(@Param("projectId") Long projectId);

    @Query("SELECT t FROM TicketJpaEntity t LEFT JOIN FETCH t.project WHERE t.id = :id")
    Optional<TicketJpaEntity> findByIdWithProject(@Param("id") Long id);
}
