package TicketCodeIA.application.usecase.chat;

import TicketCodeIA.application.query.ChatMessageResult;
import TicketCodeIA.domain.port.out.ChatMessageRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetChatHistoryUseCase {

    private final ChatMessageRepositoryPort chatMessageRepository;

    public List<ChatMessageResult> bySession(String sessionId) {
        return chatMessageRepository.findBySessionIdOrderByCreatedAt(sessionId).stream()
                .map(ChatMessageResult::fromDomain).toList();
    }

    public List<ChatMessageResult> byProject(Long projectId) {
        return chatMessageRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(ChatMessageResult::fromDomain).toList();
    }
}
