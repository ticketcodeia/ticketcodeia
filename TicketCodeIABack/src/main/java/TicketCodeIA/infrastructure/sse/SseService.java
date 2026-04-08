package TicketCodeIA.infrastructure.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SseService {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SseEmitter subscribe() {
        String clientId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitter.onCompletion(() -> {
            emitters.remove(clientId);
            log.info("SSE client disconnected: {}", clientId);
        });

        emitter.onTimeout(() -> {
            emitters.remove(clientId);
            log.info("SSE client timed out: {}", clientId);
        });

        emitter.onError(e -> {
            emitters.remove(clientId);
            log.error("SSE error for client {}: {}", clientId, e.getMessage());
        });

        emitters.put(clientId, emitter);
        log.info("SSE client connected: {}", clientId);

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"message\":\"Connected to SSE stream\"}"));
        } catch (IOException e) {
            log.error("Error sending initial SSE event: {}", e.getMessage());
        }

        return emitter;
    }

    public void broadcast(SseEvent event) {
        String jsonData;
        try {
            jsonData = objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            log.error("Error serializing SSE event: {}", e.getMessage());
            return;
        }

        emitters.forEach((clientId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(jsonData));
            } catch (IOException e) {
                log.error("Error sending SSE event to client {}: {}", clientId, e.getMessage());
                emitters.remove(clientId);
            }
        });
    }

    public int getConnectedClients() {
        return emitters.size();
    }
}
