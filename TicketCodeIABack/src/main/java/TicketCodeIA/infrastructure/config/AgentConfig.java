package TicketCodeIA.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Runtime-mutable configuration for agents.
 * Values are initialized from application.properties but can be changed at runtime via REST API.
 */
@Component
@Getter
@Setter
public class AgentConfig {

    @Value("${tickcode.workspace:C:/tickcode-workspace}")
    private String workspace;

    @Value("${claude.cli.model:claude-sonnet-4-5}")
    private String cliModel;

    @Value("${tickcode.agents.claude-code-max-turns:40}")
    private int maxTurns;

    @Value("${tickcode.agents.max-retries:3}")
    private int maxRetries;

    @Value("${spring.ai.anthropic.chat.options.model:claude-sonnet-4-5}")
    private String apiModel;

    @Value("${spring.ai.anthropic.chat.options.max-tokens:8192}")
    private int maxTokens;

    // Developer agent specific
    private String allowedTools = "Read,Write,Edit,Bash";
    private int timeoutMinutes = 5;
    private boolean dangerouslySkipPermissions = true;
}
