package TicketCodeIA.application.usecase.agent;

import TicketCodeIA.application.port.out.EventPublisherPort;
import TicketCodeIA.application.query.TicketResult;
import TicketCodeIA.domain.enums.AgentType;
import TicketCodeIA.domain.enums.TicketStatus;
import TicketCodeIA.domain.event.TicketStatusChangedEvent;
import TicketCodeIA.domain.model.agent.POAgent;
import TicketCodeIA.domain.model.agentlog.AgentLog;
import TicketCodeIA.domain.model.project.Project;
import TicketCodeIA.domain.model.ticket.Ticket;
import TicketCodeIA.domain.port.in.POAgentPort;
import TicketCodeIA.domain.port.out.AgentLogRepositoryPort;
import TicketCodeIA.domain.port.out.ProjectRepositoryPort;
import TicketCodeIA.domain.port.out.TicketRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GenerateTicketsFromRequirementsUseCase {

    private final POAgentPort poAgentPort;
    private final TicketRepositoryPort ticketRepository;
    private final ProjectRepositoryPort projectRepository;
    private final AgentLogRepositoryPort agentLogRepository;
    private final EventPublisherPort eventPublisher;

    private final POAgent poAgent = new POAgent();

    @Transactional
    public List<TicketResult> execute(String requirements, Long projectId) {
        log.info("GenerateTickets: Processing requirements to generate tickets");

        // Infrastructure adapter handles the AI call
        List<Map<String, String>> ticketData = poAgentPort.generateTicketData(requirements);

        String projectName = null;
        if (projectId != null) {
            Project project = projectRepository.findById(projectId).orElse(null);
            if (project != null) {
                projectName = project.getName();
            }
        }

        // Domain aggregate handles ticket creation logic (validation, priority parsing, defaults)
        List<Ticket> tickets = poAgent.createTicketsFromData(ticketData, projectId, projectName);

        List<TicketResult> results = new ArrayList<>();
        for (Ticket ticket : tickets) {
            ticket.setAssignedAgent(AgentType.PO);
            Ticket saved = ticketRepository.save(ticket);

            agentLogRepository.save(AgentLog.create(
                    saved.getId(), AgentType.PO, "TICKET_CREATED",
                    "Created ticket: " + saved.getTitle()));

            eventPublisher.publish(new TicketStatusChangedEvent(
                    saved.getId(), null, TicketStatus.TODO, AgentType.PO,
                    "New ticket created: " + saved.getTitle()));

            results.add(TicketResult.fromDomain(saved));
        }

        log.info("GenerateTickets: Created {} tickets", results.size());
        return results;
    }
}
