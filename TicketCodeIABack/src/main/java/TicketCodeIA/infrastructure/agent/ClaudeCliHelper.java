package TicketCodeIA.infrastructure.agent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ClaudeCliHelper {

	@Value("${tickcode.workspace}")
	private String workspacePath;

	@Value("${tickcode.agents.claude-code-max-turns}")
	private int maxTurns;

	public String getWorkspacePath() {
		return workspacePath;
	}

	public int getMaxTurns() {
		return maxTurns;
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

	public String summarize(String output) {
		if (output == null || output.isBlank())
			return "(no output)";
		return output.length() > 500 ? output.substring(0, 500) + "..." : output;
	}
}
