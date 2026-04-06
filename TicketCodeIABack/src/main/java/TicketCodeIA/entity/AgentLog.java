package TicketCodeIA.entity;

import TicketCodeIA.enums.AgentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ticketId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgentType agentType;

    @Column(nullable = false)
    private String action;

    @Column(columnDefinition = "TEXT")
    private String message;

    @CreationTimestamp
    private LocalDateTime timestamp;
}
