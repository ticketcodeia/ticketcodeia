package TicketCodeIA.application.usecase.chat;

import TicketCodeIA.domain.port.out.ChatMessageRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClearChatSessionUseCase {

    private final ChatMessageRepositoryPort chatMessageRepository;

    public void execute(String sessionId) {
        chatMessageRepository.deleteBySessionId(sessionId);
        log.info("ClearChatSession: cleared session {}", sessionId);
    }
}
