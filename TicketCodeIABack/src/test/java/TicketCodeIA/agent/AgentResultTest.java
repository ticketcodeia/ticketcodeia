package TicketCodeIA.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentResultTest {

    @Test
    void success_createsSuccessResult() {
        AgentResult result = AgentResult.success("done");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isFailure()).isFalse();
        assertThat(result.needsChanges()).isFalse();
        assertThat(result.getMessage()).isEqualTo("done");
        assertThat(result.getStatus()).isEqualTo(AgentResult.Status.SUCCESS);
    }

    @Test
    void failure_createsFailureResult() {
        AgentResult result = AgentResult.failure("error occurred");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.isFailure()).isTrue();
        assertThat(result.needsChanges()).isFalse();
        assertThat(result.getMessage()).isEqualTo("error occurred");
        assertThat(result.getStatus()).isEqualTo(AgentResult.Status.FAILURE);
    }

    @Test
    void needsChanges_createsNeedsChangesResult() {
        AgentResult result = AgentResult.needsChanges("fix indentation");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.isFailure()).isFalse();
        assertThat(result.needsChanges()).isTrue();
        assertThat(result.getMessage()).isEqualTo("fix indentation");
        assertThat(result.getStatus()).isEqualTo(AgentResult.Status.NEEDS_CHANGES);
    }

    @Test
    void builder_setsAllFields() {
        AgentResult result = AgentResult.builder()
                .status(AgentResult.Status.SUCCESS)
                .message("custom message")
                .build();
        assertThat(result.getMessage()).isEqualTo("custom message");
        assertThat(result.isSuccess()).isTrue();
    }
}
