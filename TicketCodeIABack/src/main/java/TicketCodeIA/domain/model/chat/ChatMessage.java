package TicketCodeIA.domain.model.chat;

import java.time.LocalDateTime;

public class ChatMessage {

    private Long id;
    private String sessionId;
    private Long projectId;
    private String role; // "user" or "assistant"
    private String content;
    private LocalDateTime createdAt;

    public ChatMessage(Long id, String sessionId, Long projectId, String role, String content, LocalDateTime createdAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.projectId = projectId;
        this.role = role;
        this.content = content;
        this.createdAt = createdAt;
    }

    public static ChatMessage createUser(String sessionId, Long projectId, String content) {
        return new ChatMessage(null, sessionId, projectId, "user", content, null);
    }

    public static ChatMessage createAssistant(String sessionId, Long projectId, String content) {
        return new ChatMessage(null, sessionId, projectId, "assistant", content, null);
    }

    public Long getId() { return id; }
    public String getSessionId() { return sessionId; }
    public Long getProjectId() { return projectId; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
