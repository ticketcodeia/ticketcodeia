package TicketCodeIA.infrastructure.agent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import TicketCodeIA.infrastructure.config.AgentConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClaudeCliHelper {

	private final AgentConfig agentConfig;

	public String getWorkspacePath() {
		return agentConfig.getWorkspace();
	}

	public int getMaxTurns() {
		return agentConfig.getMaxTurns();
	}

	public String getModel() {
		return agentConfig.getCliModel();
	}

	public String getAllowedTools() {
		return agentConfig.getAllowedTools();
	}

	public int getTimeoutMinutes() {
		return agentConfig.getTimeoutMinutes();
	}

	public boolean isDangerouslySkipPermissions() {
		return agentConfig.isDangerouslySkipPermissions();
	}

	public String findClaudeExecutable() {
		boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

		if (isWindows) {
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

			try {
				Process where = new ProcessBuilder("cmd", "/c", "where", "claude").redirectErrorStream(true).start();
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

			return "claude.cmd";
		}

		return "claude";
	}

	public List<String> buildClaudeCommand(String prompt) {
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
		command.add("--model");
		command.add(getModel());
		command.add(prompt);
		command.add("--output-format");
		command.add("text");
		command.add("--max-turns");
		command.add(String.valueOf(getMaxTurns()));
		command.add("--allowedTools");
		command.add(getAllowedTools());
		if (isDangerouslySkipPermissions()) {
			command.add("--dangerously-skip-permissions");
		}

		return command;
	}

	public String summarize(String output) {
		if (output == null || output.isBlank())
			return "(no output)";
		return output.length() > 500 ? output.substring(0, 500) + "..." : output;
	}
}
