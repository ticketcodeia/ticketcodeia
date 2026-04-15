package TicketCodeIA.presentation.rest;

import TicketCodeIA.infrastructure.config.AgentConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigController {

    private final AgentConfig agentConfig;

    @GetMapping("/agents")
    public Map<String, Object> getConfig() {
        return Map.of(
                "workspace", agentConfig.getWorkspace(),
                "cliModel", agentConfig.getCliModel(),
                "maxTurns", agentConfig.getMaxTurns(),
                "maxRetries", agentConfig.getMaxRetries(),
                "apiModel", agentConfig.getApiModel(),
                "maxTokens", agentConfig.getMaxTokens(),
                "allowedTools", agentConfig.getAllowedTools(),
                "timeoutMinutes", agentConfig.getTimeoutMinutes(),
                "dangerouslySkipPermissions", agentConfig.isDangerouslySkipPermissions()
        );
    }

    @PutMapping("/agents")
    public Map<String, Object> updateConfig(@RequestBody Map<String, Object> updates) {
        if (updates.containsKey("workspace")) {
            agentConfig.setWorkspace((String) updates.get("workspace"));
        }
        if (updates.containsKey("cliModel")) {
            agentConfig.setCliModel((String) updates.get("cliModel"));
        }
        if (updates.containsKey("maxTurns")) {
            agentConfig.setMaxTurns(((Number) updates.get("maxTurns")).intValue());
        }
        if (updates.containsKey("maxRetries")) {
            agentConfig.setMaxRetries(((Number) updates.get("maxRetries")).intValue());
        }
        if (updates.containsKey("apiModel")) {
            agentConfig.setApiModel((String) updates.get("apiModel"));
        }
        if (updates.containsKey("maxTokens")) {
            agentConfig.setMaxTokens(((Number) updates.get("maxTokens")).intValue());
        }
        if (updates.containsKey("allowedTools")) {
            agentConfig.setAllowedTools((String) updates.get("allowedTools"));
        }
        if (updates.containsKey("timeoutMinutes")) {
            agentConfig.setTimeoutMinutes(((Number) updates.get("timeoutMinutes")).intValue());
        }
        if (updates.containsKey("dangerouslySkipPermissions")) {
            agentConfig.setDangerouslySkipPermissions((Boolean) updates.get("dangerouslySkipPermissions"));
        }
        return getConfig();
    }
}
