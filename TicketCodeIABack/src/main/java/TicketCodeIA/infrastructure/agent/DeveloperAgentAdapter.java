package TicketCodeIA.infrastructure.agent;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import TicketCodeIA.domain.model.ticket.Ticket;
import TicketCodeIA.domain.port.in.DeveloperAgentPort;
import TicketCodeIA.domain.valueobject.AgentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeveloperAgentAdapter implements DeveloperAgentPort {

	private final ClaudeCliHelper cliHelper;

	@Override
	public AgentResult process(Ticket ticket) {
		log.info("Developer Agent: Processing ticket {}", ticket.getId());

		try {
			// Build project-specific workspace
			String projectFolder = (ticket.getProjectName() != null)
					? ticket.getProjectName().replaceAll("[^a-zA-Z0-9_\\-]", "_")
					: "ticket-" + ticket.getId();
			File workspace = new File(cliHelper.getWorkspacePath(), projectFolder);
			if (!workspace.exists()) {
				workspace.mkdirs();
			}

			String claudeExe = cliHelper.findClaudeExecutable();
			log.info("Developer Agent: Using Claude CLI: {}", claudeExe);

			String claudePrompt = String.format(
					"Implement this feature: %s\n\nDescription: %s\n\n"
							+ "Create the necessary files with clean code. Save all files in the current directory.",
					ticket.getTitle(), ticket.getDescription());

			ProcessBuilder pb = new ProcessBuilder(claudeExe, "-p", claudePrompt, "--output-format", "text",
					"--max-turns", String.valueOf(cliHelper.getMaxTurns()), "--allowedTools", "Read,Write,Edit,Bash",
					"--dangerously-skip-permissions");
			pb.directory(workspace);
			pb.redirectErrorStream(true);
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
				return AgentResult.failure("Claude Code CLI timed out after 5 minutes");
			}

			int exitCode = process.exitValue();
			String outputStr = output.toString();

			if (exitCode == 0) {
				return AgentResult.success("Implementation completed in " + workspace.getAbsolutePath() + "\n"
						+ cliHelper.summarize(outputStr));
			} else {
				return AgentResult
						.failure("Claude CLI exited with code " + exitCode + ": " + cliHelper.summarize(outputStr));
			}

		} catch (Exception e) {
			log.error("Developer Agent error for ticket {}", ticket.getId(), e);
			return AgentResult.failure(e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}
}
