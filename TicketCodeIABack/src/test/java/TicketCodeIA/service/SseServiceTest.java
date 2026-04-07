package TicketCodeIA.service;

import TicketCodeIA.dto.SseEvent;
import TicketCodeIA.enums.AgentType;
import TicketCodeIA.enums.TicketStatus;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;

class SseServiceTest {

    private final SseService sseService = new SseService();

    @Test
    void subscribe_returnsNonNullEmitter() {
        SseEmitter emitter = sseService.subscribe();
        assertThat(emitter).isNotNull();
    }

    @Test
    void subscribe_incrementsConnectedClients() {
        int before = sseService.getConnectedClients();
        sseService.subscribe();
        assertThat(sseService.getConnectedClients()).isEqualTo(before + 1);
    }

    @Test
    void subscribe_multipleClients_allRegistered() {
        sseService.subscribe();
        sseService.subscribe();
        sseService.subscribe();
        assertThat(sseService.getConnectedClients()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void getConnectedClients_initiallyZero() {
        SseService freshService = new SseService();
        assertThat(freshService.getConnectedClients()).isZero();
    }

    @Test
    void broadcast_withNoClients_doesNotThrow() {
        SseEvent event = SseEvent.ticketUpdated(1L, TicketStatus.DONE, AgentType.DEVELOPER, "Done");
        // should not throw even with no subscribers
        sseService.broadcast(event);
    }

    @Test
    void broadcast_withSubscriber_sendsWithoutException() {
        SseEmitter emitter = sseService.subscribe();
        SseEvent event = SseEvent.ticketUpdated(2L, TicketStatus.IN_PROGRESS, AgentType.DEVELOPER, "Working");
        // broadcast should work; emitter is open
        sseService.broadcast(event);
        assertThat(emitter).isNotNull();
    }
}
