package TicketCodeIA.presentation.rest;

import TicketCodeIA.infrastructure.agent.ClaudeCliHelper;
import lombok.RequiredArgsConstructor;
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

    private final ClaudeCliHelper claudeCliHelper;

    @GetMapping("/claude")
    public Map<String, Object> checkClaude() {
        Map<String, Object> result = new LinkedHashMap<>();

        File workspace = new File(claudeCliHelper.getWorkspacePath());
        result.put("workspace", workspace.getAbsolutePath());
        result.put("workspaceExists", workspace.exists());
        result.put("workspaceWritable", workspace.exists() && workspace.canWrite());

        String claudeExe = claudeCliHelper.findClaudeExecutable();
        result.put("claudeExecutable", claudeExe);

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

        result.put("PATH", System.getenv("PATH"));
        return result;
    }
}
