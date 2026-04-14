package TicketCodeIA.application.usecase.agent;

import TicketCodeIA.application.port.out.EventPublisherPort;
import TicketCodeIA.application.query.TicketResult;
import TicketCodeIA.domain.enums.AgentType;
import TicketCodeIA.domain.enums.Priority;
import TicketCodeIA.domain.enums.TicketStatus;
import TicketCodeIA.domain.event.TicketStatusChangedEvent;
import TicketCodeIA.domain.model.agentlog.AgentLog;
import TicketCodeIA.domain.model.project.Project;
import TicketCodeIA.domain.model.ticket.Ticket;
import TicketCodeIA.domain.port.out.AgentLogRepositoryPort;
import TicketCodeIA.domain.port.out.ProjectRepositoryPort;
import TicketCodeIA.domain.port.out.TicketRepositoryPort;
import TicketCodeIA.application.command.CreateTicketData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreateTicketsUseCase {

    private final TicketRepositoryPort ticketRepository;
    private final ProjectRepositoryPort projectRepository;
    private final AgentLogRepositoryPort agentLogRepository;
    private final EventPublisherPort eventPublisher;

    @Transactional
    public List<TicketResult> execute(List<CreateTicketData> ticketDataList, Long projectId) {
        log.info("CreateTickets: Creating {} tickets for project {}", ticketDataList.size(), projectId);

        String projectName = null;
        if (projectId != null) {
            Project project = projectRepository.findById(projectId).orElse(null);
            if (project != null) {
                projectName = project.getName();
            }
        }

        List<TicketResult> results = new ArrayList<>();
        for (CreateTicketData data : ticketDataList) {
            Priority priority = parsePriority(data.priority());
            Ticket ticket = Ticket.create(
                    data.title() != null ? data.title() : "Untitled",
                    data.description() != null ? data.description() : "",
                    priority,
                    projectId,
                    projectName);
            ticket.setEnableCodeReview(data.enableCodeReview());
            ticket.setEnableTesting(data.enableTesting());
            ticket.setAssignedAgent(AgentType.EXPERT);

            Ticket saved = ticketRepository.save(ticket);

            agentLogRepository.save(AgentLog.create(
                    saved.getId(), AgentType.EXPERT, "TICKET_CREATED",
                    "Created ticket: " + saved.getTitle()));

            eventPublisher.publish(new TicketStatusChangedEvent(
                    saved.getId(), null, TicketStatus.TODO, AgentType.EXPERT,
                    "New ticket created: " + saved.getTitle()));

            results.add(TicketResult.fromDomain(saved));
        }

        log.info("CreateTickets: Created {} tickets", results.size());
        return results;
    }

    private Priority parsePriority(String priorityStr) {
        if (priorityStr == null || priorityStr.isBlank()) {
            return Priority.MEDIUM;
        }
        try {
            return Priority.valueOf(priorityStr.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return Priority.MEDIUM;
        }
    }
}
