package TicketCodeIA.domain.model.ticket;

import TicketCodeIA.domain.enums.AgentType;
import TicketCodeIA.domain.enums.Priority;
import TicketCodeIA.domain.enums.TicketStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Ticket {

    private Long id;
    private String title;
    private String description;
    private TicketStatus status;
    private Priority priority;
    private AgentType assignedAgent;
    private List<String> agentLogs;
    private Long projectId;
    private String projectName;
    private String branchName;
    private boolean enableCodeReview;
    private boolean enableTesting;
    private int retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Ticket(Long id, String title, String description, TicketStatus status, Priority priority,
                  AgentType assignedAgent, List<String> agentLogs, Long projectId, String projectName,
                  String branchName, boolean enableCodeReview, boolean enableTesting, int retryCount,
                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status != null ? status : TicketStatus.TODO;
        this.priority = priority != null ? priority : Priority.MEDIUM;
        this.assignedAgent = assignedAgent;
        this.agentLogs = agentLogs != null ? new ArrayList<>(agentLogs) : new ArrayList<>();
        this.projectId = projectId;
        this.projectName = projectName;
        this.branchName = branchName;
        this.enableCodeReview = enableCodeReview;
        this.enableTesting = enableTesting;
        this.retryCount = retryCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Ticket create(String title, String description, Priority priority, Long projectId, String projectName) {
        return new Ticket(null, title, description, TicketStatus.TODO, priority != null ? priority : Priority.MEDIUM,
                null, new ArrayList<>(), projectId, projectName, null, false, false, 0, null, null);
    }

    // ── State Machine Methods ─────────────────────────────────────────────────

    public void startDevelopment() {
        this.status = TicketStatus.IN_PROGRESS;
        this.assignedAgent = AgentType.DEVELOPER;
        addLog("Developer Agent started");
    }

    public void completeDevelopment() {
        this.status = TicketStatus.CODE_REVIEW;
        addLog("Development completed");
    }

    public void assignReviewer() {
        this.assignedAgent = AgentType.REVIEWER;
    }

    public void approveReview() {
        this.status = TicketStatus.TESTING;
        addLog("Code review approved");
    }

    public void requestChanges(String comments) {
        this.status = TicketStatus.IN_PROGRESS;
        this.retryCount++;
        addLog("Changes requested: " + comments);
    }

    public void skipToTesting() {
        this.status = TicketStatus.TESTING;
        addLog("Skipped to testing (review disabled)");
    }

    public void assignTester() {
        this.assignedAgent = AgentType.TESTER;
    }

    public void completeTests() {
        this.status = TicketStatus.DONE;
        addLog("All tests passed");
    }

    public void failTests(String reason) {
        this.status = TicketStatus.IN_PROGRESS;
        this.retryCount++;
        addLog("Tests failed: " + reason);
    }

    public void escalate(String reason) {
        this.status = TicketStatus.ESCALATED;
        this.assignedAgent = AgentType.HUMAN;
        addLog("Escalated: " + reason);
    }

    public void escalateToHuman(AgentType failingAgent, String reason) {
        this.assignedAgent = AgentType.HUMAN;
        switch (failingAgent) {
            case DEVELOPER -> {
                this.status = TicketStatus.HUMAN_DEV;
                addLog("Escalated to human developer: " + reason);
            }
            case REVIEWER -> {
                this.status = TicketStatus.HUMAN_REVIEW;
                addLog("Escalated to human reviewer: " + reason);
            }
            case TESTER -> {
                this.status = TicketStatus.HUMAN_TESTING;
                addLog("Escalated to human tester: " + reason);
            }
            default -> {
                this.status = TicketStatus.HUMAN_TODO;
                addLog("Escalated to human board: " + reason);
            }
        }
    }

    public void markDone(String reason) {
        this.status = TicketStatus.DONE;
        addLog("Marked done: " + reason);
    }

    public boolean isInFinalState() {
        return status == TicketStatus.DONE || status == TicketStatus.ESCALATED || isOnHumanBoard();
    }

    public boolean isOnHumanBoard() {
        return status == TicketStatus.HUMAN_TODO || status == TicketStatus.HUMAN_DEV
                || status == TicketStatus.HUMAN_REVIEW || status == TicketStatus.HUMAN_TESTING;
    }

    // ── Human Board State Machine ────────────────────────────────────────────

    public void moveToHumanBoard() {
        if (this.status != TicketStatus.ESCALATED) {
            throw new IllegalStateException("Only escalated tickets can be moved to human board. Current: " + status);
        }
        this.status = TicketStatus.HUMAN_TODO;
        addLog("Moved to human board");
    }

    public void startHumanDevelopment() {
        if (this.status != TicketStatus.HUMAN_TODO) {
            throw new IllegalStateException("Cannot start human dev from status: " + status);
        }
        this.status = TicketStatus.HUMAN_DEV;
        this.assignedAgent = AgentType.HUMAN;
        addLog("Human developer started");
    }

    public void completeHumanDevelopment() {
        if (this.status != TicketStatus.HUMAN_DEV) {
            throw new IllegalStateException("Cannot complete human dev from status: " + status);
        }
        this.status = TicketStatus.HUMAN_REVIEW;
        addLog("Human development completed, moved to review");
    }

    public void completeHumanReview() {
        if (this.status != TicketStatus.HUMAN_REVIEW) {
            throw new IllegalStateException("Cannot complete human review from status: " + status);
        }
        this.status = TicketStatus.HUMAN_TESTING;
        addLog("Human review completed, moved to testing");
    }

    public void completeHumanTesting() {
        if (this.status != TicketStatus.HUMAN_TESTING) {
            throw new IllegalStateException("Cannot complete human testing from status: " + status);
        }
        this.status = TicketStatus.DONE;
        addLog("Human testing completed, ticket done");
    }

    public boolean hasExceededRetries(int maxRetries) {
        return retryCount >= maxRetries;
    }

    // ── Log Management ────────────────────────────────────────────────────────

    public void addLog(String log) {
        if (this.agentLogs == null) {
            this.agentLogs = new ArrayList<>();
        }
        this.agentLogs.add(log);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public TicketStatus getStatus() { return status; }
    public Priority getPriority() { return priority; }
    public AgentType getAssignedAgent() { return assignedAgent; }
    public List<String> getAgentLogs() { return Collections.unmodifiableList(agentLogs); }
    public Long getProjectId() { return projectId; }
    public String getProjectName() { return projectName; }
    public String getBranchName() { return branchName; }
    public boolean isEnableCodeReview() { return enableCodeReview; }
    public boolean isEnableTesting() { return enableTesting; }
    public int getRetryCount() { return retryCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // ── Setters (for use case orchestration and mapping) ──────────────────────

    public void setId(Long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setStatus(TicketStatus status) { this.status = status; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public void setAssignedAgent(AgentType assignedAgent) { this.assignedAgent = assignedAgent; }
    public void setBranchName(String branchName) { this.branchName = branchName; }
    public void setEnableCodeReview(boolean enableCodeReview) { this.enableCodeReview = enableCodeReview; }
    public void setEnableTesting(boolean enableTesting) { this.enableTesting = enableTesting; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
