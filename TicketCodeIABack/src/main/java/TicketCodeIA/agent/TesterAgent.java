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
public class TesterAgent {

    private final TicketService ticketService;
    private final AgentLogService agentLogService;
    private final SseService sseService;

    @Value("${tickcode.workspace:/tmp/tickcode-workspace}")
    private String workspacePath;

    @Value("${tickcode.agents.claude-code-max-turns:10}")
    private int maxTurns;

    public AgentResult process(Ticket ticket) {
        log.info("Tester Agent: Testing ticket {}", ticket.getId());

        ticket.setAssignedAgent(AgentType.TESTER);
        ticket.addAgentLog("Tester Agent started testing");
        ticketService.saveTicket(ticket);

        agentLogService.log(ticket.getId(), AgentType.TESTER, "STARTED",
                "Started testing: " + ticket.getTitle());

        sseService.broadcast(SseEvent.ticketUpdated(
                ticket.getId(),
                TicketStatus.TESTING,
                AgentType.TESTER,
                "Testing in progress"
        ));

        try {
            File workspace = new File(workspacePath);
            if (!workspace.exists()) {
                workspace.mkdirs();
            }

            String claudePrompt = String.format(
                    "Write and run unit tests for the recent changes related to: %s\n\nDescription: %s\n\n" +
                    "Create test files and execute them. Report if tests pass or fail.",
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
                throw new RuntimeException("Claude Code CLI timed out during testing");
            }

            String outputStr = output.toString().toLowerCase();
            boolean testsPass = exitCode == 0 &&
                    (outputStr.contains("pass") || outputStr.contains("success")) &&
                    !outputStr.contains("fail");

            if (testsPass) {
                ticket.setStatus(TicketStatus.DONE);
                ticket.addAgentLog("Tester Agent: All tests passed");
                ticketService.saveTicket(ticket);

                agentLogService.log(ticket.getId(), AgentType.TESTER, "COMPLETED",
                        "All tests passed, ticket completed");

                sseService.broadcast(SseEvent.ticketUpdated(
                        ticket.getId(),
                        TicketStatus.DONE,
                        AgentType.TESTER,
                        "All tests passed! Ticket completed"
                ));

                return AgentResult.success("All tests passed");
            } else {
                String failureMsg = "Tests failed or incomplete";
                ticket.addAgentLog("Tester Agent: " + failureMsg);
                ticketService.saveTicket(ticket);

                agentLogService.log(ticket.getId(), AgentType.TESTER, "FAILED", failureMsg);

                sseService.broadcast(SseEvent.ticketUpdated(
                        ticket.getId(),
                        TicketStatus.TESTING,
                        AgentType.TESTER,
                        "Tests failed, needs fixing"
                ));

                return AgentResult.failure(failureMsg);
            }

        } catch (Exception e) {
            log.error("Tester Agent: Error testing ticket {}", ticket.getId(), e);

            ticket.addAgentLog("Tester Agent error: " + e.getMessage());
            ticketService.saveTicket(ticket);

            agentLogService.log(ticket.getId(), AgentType.TESTER, "ERROR", e.getMessage());

            return AgentResult.failure(e.getMessage());
        }
    }
}
