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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeveloperAgent {

    private final TicketService ticketService;
    private final AgentLogService agentLogService;
    private final SseService sseService;

    @Value("${tickcode.workspace:C:/tickcode-workspace}")
    private String workspacePath;

    @Value("${tickcode.agents.claude-code-max-turns:10}")
    private int maxTurns;

    public AgentResult process(Ticket ticket) {
        log.info("Developer Agent: Processing ticket {}", ticket.getId());

        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticket.setAssignedAgent(AgentType.DEVELOPER);
        ticket.addAgentLog("Developer Agent started");
        ticketService.saveTicket(ticket);

        agentLogService.log(ticket.getId(), AgentType.DEVELOPER, "STARTED",
                "Started implementing: " + ticket.getTitle());

        sseService.broadcast(SseEvent.ticketUpdated(
                ticket.getId(), TicketStatus.IN_PROGRESS, AgentType.DEVELOPER, "Development started"));

        try {
            // Build project-specific workspace: C:/tickcode-workspace/{project-name}
            String projectFolder = (ticket.getProject() != null && ticket.getProject().getName() != null)
                    ? ticket.getProject().getName().replaceAll("[^a-zA-Z0-9_\\-]", "_")
                    : "ticket-" + ticket.getId();
            File workspace = new File(workspacePath, projectFolder);
            if (!workspace.exists()) {
                workspace.mkdirs();
            }
            ticket.setBranchName("feature/ticket-" + ticket.getId());

            // Find claude executable
            String claudeExe = findClaudeExecutable();
            String logMsg = "Using Claude CLI: " + claudeExe;
            log.info("Developer Agent: {}", logMsg);
            ticket.addAgentLog(logMsg);
            ticketService.saveTicket(ticket);

            String claudePrompt = String.format(
                    "Implement this feature: %s\n\nDescription: %s\n\n" +
                    "Create the necessary files with clean code. Save all files in the current directory.",
                    ticket.getTitle(), ticket.getDescription());

            ProcessBuilder pb = new ProcessBuilder(
                    claudeExe,
                    "-p", claudePrompt,
                    "--output-format", "text",
                    "--max-turns", String.valueOf(maxTurns),
                    "--allowedTools", "Read,Write,Edit,Bash",
                    "--dangerously-skip-permissions"
            );
            pb.directory(workspace);
            pb.redirectErrorStream(true);

            // Pass current environment so PATH is inherited
            pb.environment().putAll(System.getenv());

            log.info("Developer Agent: Starting process in {}", workspace.getAbsolutePath());
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    log.info("Claude CLI output: {}", line);
                }
            }

            boolean completed = process.waitFor(5, TimeUnit.MINUTES);

            if (!completed) {
                process.destroyForcibly();
                return fail(ticket, "Claude Code CLI timed out after 5 minutes");
            }

            int exitCode = process.exitValue();
            String outputStr = output.toString();

            ticket.addAgentLog("Claude CLI exit code: " + exitCode);
            ticket.addAgentLog("Claude CLI output (truncated): " + summarize(outputStr));
            ticketService.saveTicket(ticket);

            if (exitCode == 0) {
                ticket.setStatus(TicketStatus.CODE_REVIEW);
                ticketService.saveTicket(ticket);

                agentLogService.log(ticket.getId(), AgentType.DEVELOPER, "COMPLETED",
                        "Implementation done, files in: " + workspace.getAbsolutePath());

                sseService.broadcast(SseEvent.ticketUpdated(
                        ticket.getId(), TicketStatus.CODE_REVIEW, AgentType.DEVELOPER,
                        "Development completed, ready for review"));

                return AgentResult.success("Implementation completed in " + workspace.getAbsolutePath());
            } else {
                return fail(ticket, "Claude CLI exited with code " + exitCode + ": " + summarize(outputStr));
            }

        } catch (Exception e) {
            log.error("Developer Agent error for ticket {}", ticket.getId(), e);
            return fail(ticket, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /**
     * Finds the claude executable. On Windows, npm global installs go to
     * %APPDATA%\npm\claude.cmd so we search there first before falling back to PATH.
     */
    public String findClaudeExecutable() {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        if (isWindows) {
            // Common Windows install locations
            String appData = System.getenv("APPDATA");
            if (appData != null) {
                Path npmClaude = Paths.get(appData, "npm", "claude.cmd");
                if (Files.exists(npmClaude)) {
                    return npmClaude.toAbsolutePath().toString();
                }
            }

            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null) {
                Path npmClaude = Paths.get(localAppData, "npm", "claude.cmd");
                if (Files.exists(npmClaude)) {
                    return npmClaude.toAbsolutePath().toString();
                }
            }

            // Try resolving via PATH using where.exe
            try {
                Process where = new ProcessBuilder("cmd", "/c", "where", "claude")
                        .redirectErrorStream(true).start();
                String result = new String(where.getInputStream().readAllBytes()).trim();
                where.waitFor(5, TimeUnit.SECONDS);
                if (!result.isBlank()) {
                    String firstLine = result.lines().findFirst().orElse("claude").trim();
                    log.info("Found claude via where.exe: {}", firstLine);
                    return firstLine;
                }
            } catch (Exception e) {
                log.warn("Could not run where.exe to find claude: {}", e.getMessage());
            }

            // Last resort: hope it's on PATH
            return "claude.cmd";
        }

        return "claude";
    }

    private AgentResult fail(Ticket ticket, String reason) {
        ticket.addAgentLog("Developer Agent FAILED: " + reason);
        ticketService.saveTicket(ticket);
        agentLogService.log(ticket.getId(), AgentType.DEVELOPER, "FAILED", reason);
        return AgentResult.failure(reason);
    }

    private String summarize(String output) {
        if (output == null || output.isBlank()) return "(no output)";
        return output.length() > 500 ? output.substring(0, 500) + "..." : output;
    }
}
