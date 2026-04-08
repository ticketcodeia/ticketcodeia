package TicketCodeIA.domain.model.project;

import java.time.LocalDateTime;

public class Project {

    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;

    public Project(Long id, String name, String description, LocalDateTime createdAt) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Project name must not be blank");
        }
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
    }

    public static Project create(String name, String description) {
        return new Project(null, name, description, null);
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
