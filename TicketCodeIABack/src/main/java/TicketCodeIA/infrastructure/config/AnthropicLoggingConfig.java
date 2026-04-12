package TicketCodeIA.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Provides a RestClient.Builder with logging interceptor so we can see
 * the raw HTTP bodies sent to/from the Anthropic API.
 */
@Configuration
@Slf4j
public class AnthropicLoggingConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder()
                .requestFactory(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()))
                .requestInterceptor(new AnthropicLoggingInterceptor());
    }

    @Slf4j
    static class AnthropicLoggingInterceptor implements ClientHttpRequestInterceptor {

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                ClientHttpRequestExecution execution) throws IOException {

            String url = request.getURI().toString();
            if (!url.contains("anthropic.com")) {
                return execution.execute(request, body);
            }

            String bodyStr = new String(body, StandardCharsets.UTF_8);

            // Log tools section of the request
            int toolsIdx = bodyStr.indexOf("\"tools\"");
            if (toolsIdx >= 0) {
                int end = Math.min(bodyStr.length(), toolsIdx + 3000);
                log.info(">>> ANTHROPIC REQUEST [tools]: ...{}...", bodyStr.substring(toolsIdx, end));
            }

            // Log tool_use content in responses sent back (tool results)
            if (bodyStr.contains("tool_use") || bodyStr.contains("tool_result")) {
                String truncated = bodyStr.length() > 4000 ? bodyStr.substring(0, 4000) + "..." : bodyStr;
                log.info(">>> ANTHROPIC REQUEST [tool exchange]: {}", truncated);
            }

            ClientHttpResponse response = execution.execute(request, body);

            // Read and log response body
            byte[] responseBody = response.getBody().readAllBytes();
            String responseStr = new String(responseBody, StandardCharsets.UTF_8);

            if (responseStr.contains("tool_use")) {
                // Find the tool_use section
                int tuIdx = responseStr.indexOf("tool_use");
                if (tuIdx >= 0) {
                    int start = Math.max(0, tuIdx - 50);
                    int end = Math.min(responseStr.length(), tuIdx + 1000);
                    log.info("<<< ANTHROPIC RESPONSE [tool_use]: ...{}...", responseStr.substring(start, end));
                }
            }

            // Return a wrapper that replays the already-read bytes
            return new BufferedClientHttpResponse(response, responseBody);
        }
    }

    /**
     * Wraps a response so the body can be re-read after we've already consumed it for logging.
     */
    static class BufferedClientHttpResponse implements ClientHttpResponse {
        private final ClientHttpResponse original;
        private final byte[] body;

        BufferedClientHttpResponse(ClientHttpResponse original, byte[] body) {
            this.original = original;
            this.body = body;
        }

        @Override public InputStream getBody() { return new ByteArrayInputStream(body); }
        @Override public org.springframework.http.HttpHeaders getHeaders() { return original.getHeaders(); }
        @Override public org.springframework.http.HttpStatusCode getStatusCode() throws IOException { return original.getStatusCode(); }
        @Override public String getStatusText() throws IOException { return original.getStatusText(); }
        @Override public void close() { original.close(); }
    }
}
