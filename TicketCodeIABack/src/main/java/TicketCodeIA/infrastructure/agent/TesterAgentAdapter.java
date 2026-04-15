package TicketCodeIA.infrastructure.agent;

import TicketCodeIA.domain.model.ticket.Ticket;
import TicketCodeIA.domain.port.in.TesterAgentPort;
import TicketCodeIA.domain.valueobject.AgentResult;
import TicketCodeIA.domain.valueobject.ProjectContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class TesterAgentAdapter implements TesterAgentPort {

    private final ClaudeCliHelper cliHelper;

    @Override
    public AgentResult process(Ticket ticket, ProjectContext context) {
        log.info("Tester Agent: Testing ticket {} with project context", ticket.getId());

        try {
            File workspace = new File(cliHelper.getWorkspacePath());
            if (!workspace.exists()) {
                workspace.mkdirs();
            }

            String claudePrompt = String.format(
                    "%s\n\n"
                    + "=== YOUR CURRENT TASK ===\n"
                    + "Write and run unit tests for ticket #%d: %s\n\nDescription: %s\n\n"
                    + "Create test files, execute them, and report whether they pass or fail. "
                    + "End your response with either 'ALL TESTS PASSED' or 'TESTS FAILED'.",
                    context.toPromptString(), ticket.getId(), ticket.getTitle(), ticket.getDescription());

            List<String> command = cliHelper.buildClaudeCommand(claudePrompt);
            log.info("Tester Agent: Running command: {}", String.join(" ", command));

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(workspace);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    log.debug("Claude Code: {}", line);
                }
            }

            boolean completed = process.waitFor(5, TimeUnit.MINUTES);

            if (!completed) {
                process.destroyForcibly();
                return AgentResult.failure("Claude Code CLI timed out during testing");
            }

            int exitCode = process.exitValue();
            String outputStr = output.toString().toLowerCase();
            boolean testsPass = exitCode == 0 && outputStr.contains("all tests passed");

            if (testsPass) {
                return AgentResult.success("All tests passed");
            } else {
                return AgentResult.failure("Tests failed (exit code " + exitCode + ")");
            }

        } catch (Exception e) {
            log.error("Tester Agent: Error testing ticket {}", ticket.getId(), e);
            return AgentResult.failure(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }
}
