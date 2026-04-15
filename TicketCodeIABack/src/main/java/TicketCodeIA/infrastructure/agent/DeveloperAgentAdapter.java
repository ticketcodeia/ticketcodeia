package TicketCodeIA.infrastructure.agent;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import TicketCodeIA.domain.model.ticket.Ticket;
import TicketCodeIA.domain.port.in.DeveloperAgentPort;
import TicketCodeIA.domain.valueobject.AgentResult;
import TicketCodeIA.domain.valueobject.ProjectContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeveloperAgentAdapter implements DeveloperAgentPort {

	private final ClaudeCliHelper cliHelper;

	@Override
	public AgentResult process(Ticket ticket, ProjectContext context) {
		log.info("Developer Agent: Processing ticket {} with project context", ticket.getId());

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
					"%s\n\n"
							+ "=== YOUR CURRENT TASK ===\n"
							+ "Ticket #%d: %s\n\n"
							+ "Description: %s\n\n"
							+ "Create the necessary files with clean code. Save all files in the current directory.\n"
							+ "Make sure your implementation is consistent with the other tickets in the project.",
					context.toPromptString(), ticket.getId(), ticket.getTitle(), ticket.getDescription());

			List<String> cmdArgs = new ArrayList<>(List.of(
					claudeExe, "-p", "--model", cliHelper.getModel(), claudePrompt,
					"--output-format", "text", "--max-turns", String.valueOf(cliHelper.getMaxTurns()),
					"--allowedTools", cliHelper.getAllowedTools()));
			if (cliHelper.isDangerouslySkipPermissions()) {
				cmdArgs.add("--dangerously-skip-permissions");
			}
			ProcessBuilder pb = new ProcessBuilder(cmdArgs);
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

			boolean completed = process.waitFor(cliHelper.getTimeoutMinutes(), TimeUnit.MINUTES);

			if (!completed) {
				process.destroyForcibly();
				return AgentResult.failure("Claude Code CLI timed out after " + cliHelper.getTimeoutMinutes() + " minutes");
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
