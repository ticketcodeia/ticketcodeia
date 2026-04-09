package TicketCodeIA.infrastructure.persistence.adapter;

import TicketCodeIA.domain.enums.TicketStatus;
import TicketCodeIA.domain.model.ticket.Ticket;
import TicketCodeIA.domain.port.out.TicketRepositoryPort;
import TicketCodeIA.infrastructure.persistence.entity.TicketJpaEntity;
import TicketCodeIA.infrastructure.persistence.mapper.TicketPersistenceMapper;
import TicketCodeIA.infrastructure.persistence.repository.TicketJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TicketRepositoryAdapter implements TicketRepositoryPort {

    private final TicketJpaRepository jpaRepository;
    private final TicketPersistenceMapper mapper;

    @Override
    public Ticket save(Ticket ticket) {
        TicketJpaEntity entity;
        if (ticket.getId() != null) {
            // Update existing entity to preserve managed state
            Optional<TicketJpaEntity> existing = jpaRepository.findById(ticket.getId());
            if (existing.isPresent()) {
                entity = existing.get();
                mapper.updateJpaEntity(entity, ticket);
            } else {
                entity = mapper.toJpaEntity(ticket);
            }
        } else {
            entity = mapper.toJpaEntity(ticket);
        }
        var saved = jpaRepository.save(entity);
        // Re-fetch with JOIN FETCH to eagerly load the project association
        var withProject = jpaRepository.findByIdWithProject(saved.getId()).orElse(saved);
        return mapper.toDomain(withProject);
    }

    @Override
    public Optional<Ticket> findById(Long id) {
        return jpaRepository.findByIdWithProject(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Ticket> findByIdWithProject(Long id) {
        return jpaRepository.findByIdWithProject(id).map(mapper::toDomain);
    }

    @Override
    public List<Ticket> findAll() {
        return jpaRepository.findAllWithProject().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Ticket> findByStatus(TicketStatus status) {
        return jpaRepository.findByStatus(status).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Ticket> findByProjectId(Long projectId) {
        return jpaRepository.findByProjectId(projectId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Ticket> findByProjectIdAndStatus(Long projectId, TicketStatus status) {
        return jpaRepository.findByProjectIdAndStatus(projectId, status).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<Ticket> findAllOrderByCreatedAtDesc() {
        return jpaRepository.findAllByOrderByCreatedAtDesc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Ticket> findByProjectIdOrderByCreatedAtDesc(Long projectId) {
        return jpaRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public long countByStatus(TicketStatus status) {
        return jpaRepository.countByStatus(status);
    }

    @Override
    public long countByProjectIdAndStatusIn(Long projectId, List<TicketStatus> statuses) {
        return jpaRepository.countByProjectIdAndStatusIn(projectId, statuses);
    }
}
