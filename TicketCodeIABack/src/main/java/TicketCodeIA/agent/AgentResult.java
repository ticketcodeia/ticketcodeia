package TicketCodeIA.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResult {

    public enum Status {
        SUCCESS,
        FAILURE,
        NEEDS_CHANGES
    }

    private Status status;
    private String message;

    public static AgentResult success(String message) {
        return AgentResult.builder()
                .status(Status.SUCCESS)
                .message(message)
                .build();
    }

    public static AgentResult failure(String message) {
        return AgentResult.builder()
                .status(Status.FAILURE)
                .message(message)
                .build();
    }

    public static AgentResult needsChanges(String message) {
        return AgentResult.builder()
                .status(Status.NEEDS_CHANGES)
                .message(message)
                .build();
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
}
