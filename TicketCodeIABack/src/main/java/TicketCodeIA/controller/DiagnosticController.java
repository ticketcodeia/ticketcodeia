package TicketCodeIA.controller;

import TicketCodeIA.agent.DeveloperAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/diagnostic")
@RequiredArgsConstructor
public class DiagnosticController {

    private final DeveloperAgent developerAgent;

    @Value("${tickcode.workspace:C:/tickcode-workspace}")
    private String workspacePath;

    @GetMapping("/claude")
    public Map<String, Object> checkClaude() {
        Map<String, Object> result = new LinkedHashMap<>();

        // Check workspace
        File workspace = new File(workspacePath);
        result.put("workspace", workspace.getAbsolutePath());
        result.put("workspaceExists", workspace.exists());
        result.put("workspaceWritable", workspace.exists() && workspace.canWrite());

        // Find claude
        String claudeExe = developerAgent.findClaudeExecutable();
        result.put("claudeExecutable", claudeExe);

        // Run claude --version
        try {
            ProcessBuilder pb = new ProcessBuilder(claudeExe, "--version");
            pb.redirectErrorStream(true);
            pb.environment().putAll(System.getenv());
            Process p = pb.start();
            String output = new String(p.getInputStream().readAllBytes()).trim();
            p.waitFor(10, TimeUnit.SECONDS);
            result.put("claudeVersion", output.isBlank() ? "(no output)" : output);
            result.put("claudeExitCode", p.exitValue());
            result.put("claudeAvailable", p.exitValue() == 0);
        } catch (Exception e) {
            result.put("claudeAvailable", false);
            result.put("claudeError", e.getMessage());
        }

        // Show PATH
        result.put("PATH", System.getenv("PATH"));

        return result;
    }
}
