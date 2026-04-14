package TicketCodeIA.infrastructure.agent.tools;

/**
 * Thread-local context for passing the current project ID to tool callbacks.
 */
public final class ExpertToolContext {

    private static final ThreadLocal<Long> CURRENT_PROJECT_ID = new ThreadLocal<>();

    private ExpertToolContext() {}

    public static void setCurrentProjectId(Long projectId) {
        CURRENT_PROJECT_ID.set(projectId);
    }

    public static Long getCurrentProjectId() {
        return CURRENT_PROJECT_ID.get();
    }

    public static void clearCurrentProjectId() {
        CURRENT_PROJECT_ID.remove();
    }
}
