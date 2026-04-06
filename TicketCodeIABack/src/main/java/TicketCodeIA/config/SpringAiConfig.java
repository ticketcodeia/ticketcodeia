package TicketCodeIA.config;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringAiConfig {

    @Bean
    @ConditionalOnMissingBean
    public ChatClient.Builder chatClientBuilder(AnthropicChatModel chatModel) {
        return ChatClient.builder(chatModel);
    }
}
