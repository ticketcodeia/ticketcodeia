package TicketCodeIA.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import TicketCodeIA.enums.AgentType;
import TicketCodeIA.enums.Priority;
import TicketCodeIA.enums.TicketStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tickets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String title;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private TicketStatus status = TicketStatus.TODO;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private Priority priority = Priority.MEDIUM;

	@Enumerated(EnumType.STRING)
	private AgentType assignedAgent;

	@Column(columnDefinition = "TEXT")
	@Convert(converter = StringListConverter.class)
	@Builder.Default
	private List<String> agentLogs = new ArrayList<>();

	private String branchName;

	@Builder.Default
	private boolean enableCodeReview = false;

	@Builder.Default
	private boolean enableTesting = false;

	@Builder.Default
	private int retryCount = 0;

	@CreationTimestamp
	private LocalDateTime createdAt;

	@UpdateTimestamp
	private LocalDateTime updatedAt;

	public void addAgentLog(String log) {
		if (this.agentLogs == null) {
			this.agentLogs = new ArrayList<>();
		}
		this.agentLogs.add(log);
	}
}
