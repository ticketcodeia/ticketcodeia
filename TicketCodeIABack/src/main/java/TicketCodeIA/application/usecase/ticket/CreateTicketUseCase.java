package TicketCodeIA.application.usecase.ticket;

import TicketCodeIA.application.command.CreateTicketCommand;
import TicketCodeIA.application.query.TicketResult;
import TicketCodeIA.domain.model.project.Project;
import TicketCodeIA.domain.model.ticket.Ticket;
import TicketCodeIA.domain.port.out.ProjectRepositoryPort;
import TicketCodeIA.domain.port.out.TicketRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateTicketUseCase {

    private final TicketRepositoryPort ticketRepository;
    private final ProjectRepositoryPort projectRepository;

    @Transactional
    public TicketResult execute(CreateTicketCommand command) {
        String projectName = null;
        if (command.projectId() != null) {
            Project project = projectRepository.findById(command.projectId()).orElse(null);
            if (project != null) {
                projectName = project.getName();
            }
        }

        Ticket ticket = Ticket.create(
                command.title(),
                command.description(),
                command.priority(),
                command.projectId(),
                projectName
        );

        Ticket saved = ticketRepository.save(ticket);
        return TicketResult.fromDomain(saved);
    }
}
