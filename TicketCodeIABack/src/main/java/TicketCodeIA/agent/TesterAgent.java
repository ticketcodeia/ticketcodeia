package TicketCodeIA.agent;

import TicketCodeIA.dto.SseEvent;
import TicketCodeIA.entity.Ticket;
import TicketCodeIA.enums.AgentType;
import TicketCodeIA.enums.TicketStatus;
import TicketCodeIA.service.AgentLogService;
import TicketCodeIA.service.SseService;
import TicketCodeIA.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class TesterAgent {

    private final TicketService ticketService;
    private final AgentLogService agentLogService;
    private final SseService sseService;

    @Value("${tickcode.workspace:C:/tickcode-workspace}")
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
                    "Create test files, execute them, and report whether they pass or fail. " +
                    "End your response with either 'ALL TESTS PASSED' or 'TESTS FAILED'.",
                    ticket.getTitle(),
                    ticket.getDescription()
            );

            List<String> command = buildClaudeCommand(claudePrompt);
            log.info("Tester Agent: Running command: {}", String.join(" ", command));

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(workspace);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    log.debug("Claude Code: {}", line);
                }
            }

            boolean completed = process.waitFor(5, TimeUnit.MINUTES);

            if (!completed) {
                process.destroyForcibly();
                String msg = "Claude Code CLI timed out during testing";
                ticket.addAgentLog("Tester Agent: " + msg);
                ticketService.saveTicket(ticket);
                agentLogService.log(ticket.getId(), AgentType.TESTER, "TIMEOUT", msg);
                return AgentResult.failure(msg);
            }

            int exitCode = process.exitValue();
            String outputStr = output.toString().toLowerCase();
            boolean testsPass = exitCode == 0 && outputStr.contains("all tests passed");

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
                String failureMsg = "Tests failed (exit code " + exitCode + ")";
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

            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            ticket.addAgentLog("Tester Agent error: " + errorMsg);
            ticketService.saveTicket(ticket);

            agentLogService.log(ticket.getId(), AgentType.TESTER, "ERROR", errorMsg);

            return AgentResult.failure(errorMsg);
        }
    }

    private List<String> buildClaudeCommand(String prompt) {
        List<String> command = new ArrayList<>();

        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        if (isWindows) {
            command.add("cmd");
            command.add("/c");
            command.add("claude");
        } else {
            command.add("claude");
        }

        command.add("--print");
        command.add(prompt);
        command.add("--output-format");
        command.add("text");
        command.add("--max-turns");
        command.add(String.valueOf(maxTurns));
        command.add("--allowedTools");
        command.add("Read,Write,Edit,Bash");
        command.add("--dangerously-skip-permissions");

        return command;
    }
}
