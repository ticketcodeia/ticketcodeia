package TicketCodeIA.agent;

import TicketCodeIA.entity.Ticket;
import TicketCodeIA.enums.AgentType;
import TicketCodeIA.enums.TicketStatus;
import TicketCodeIA.service.AgentLogService;
import TicketCodeIA.service.SseService;
import TicketCodeIA.service.TicketService;
import TicketCodeIA.dto.SseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeveloperAgent {

    private final TicketService ticketService;
    private final AgentLogService agentLogService;
    private final SseService sseService;

    @Value("${tickcode.workspace:/tmp/tickcode-workspace}")
    private String workspacePath;

    @Value("${tickcode.agents.claude-code-max-turns:10}")
    private int maxTurns;

    public AgentResult process(Ticket ticket) {
        log.info("Developer Agent: Processing ticket {}", ticket.getId());

        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticket.setAssignedAgent(AgentType.DEVELOPER);
        ticket.addAgentLog("Developer Agent started implementation");
        ticketService.saveTicket(ticket);

        agentLogService.log(ticket.getId(), AgentType.DEVELOPER, "STARTED",
                "Started implementing: " + ticket.getTitle());

        sseService.broadcast(SseEvent.ticketUpdated(
                ticket.getId(),
                TicketStatus.IN_PROGRESS,
                AgentType.DEVELOPER,
                "Development started"
        ));

        try {
            File workspace = new File(workspacePath);
            if (!workspace.exists()) {
                workspace.mkdirs();
            }

            String branchName = "feature/ticket-" + ticket.getId();
            ticket.setBranchName(branchName);

            String claudePrompt = String.format(
                    "Implement this feature: %s\n\nDescription: %s\n\nCreate the necessary files with clean code.",
                    ticket.getTitle(),
                    ticket.getDescription()
            );

            ProcessBuilder pb = new ProcessBuilder(
                    "claude", "-p", claudePrompt,
                    "--output-format", "json",
                    "--max-turns", String.valueOf(maxTurns),
                    "--allowedTools", "Read,Write,Edit,Bash"
            );
            pb.directory(workspace);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean completed = process.waitFor(5, TimeUnit.MINUTES);
            int exitCode = completed ? process.exitValue() : -1;

            if (!completed) {
                process.destroyForcibly();
                throw new RuntimeException("Claude Code CLI timed out");
            }

            if (exitCode == 0) {
                ticket.setStatus(TicketStatus.CODE_REVIEW);
                ticket.addAgentLog("Developer Agent completed implementation successfully");
                ticketService.saveTicket(ticket);

                agentLogService.log(ticket.getId(), AgentType.DEVELOPER, "COMPLETED",
                        "Implementation completed, moving to code review");

                sseService.broadcast(SseEvent.ticketUpdated(
                        ticket.getId(),
                        TicketStatus.CODE_REVIEW,
                        AgentType.DEVELOPER,
                        "Development completed, ready for review"
                ));

                return AgentResult.success("Implementation completed");
            } else {
                String errorMsg = "Claude Code CLI exited with code: " + exitCode;
                ticket.addAgentLog("Developer Agent failed: " + errorMsg);
                ticketService.saveTicket(ticket);

                agentLogService.log(ticket.getId(), AgentType.DEVELOPER, "FAILED", errorMsg);

                return AgentResult.failure(errorMsg);
            }

        } catch (Exception e) {
            log.error("Developer Agent: Error processing ticket {}", ticket.getId(), e);

            ticket.addAgentLog("Developer Agent error: " + e.getMessage());
            ticketService.saveTicket(ticket);

            agentLogService.log(ticket.getId(), AgentType.DEVELOPER, "ERROR", e.getMessage());

            return AgentResult.failure(e.getMessage());
        }
    }
}
