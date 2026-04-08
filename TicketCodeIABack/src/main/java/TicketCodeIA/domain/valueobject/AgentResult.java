package TicketCodeIA.domain.valueobject;

public final class AgentResult {

    public enum Status {
        SUCCESS,
        FAILURE,
        NEEDS_CHANGES
    }

    private final Status status;
    private final String message;

    private AgentResult(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    public static AgentResult success(String message) {
        return new AgentResult(Status.SUCCESS, message);
    }

    public static AgentResult failure(String message) {
        return new AgentResult(Status.FAILURE, message);
    }

    public static AgentResult needsChanges(String message) {
        return new AgentResult(Status.NEEDS_CHANGES, message);
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isFailure() {
        return status == Status.FAILURE;
    }

    public boolean needsChanges() {
        return status == Status.NEEDS_CHANGES;
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
