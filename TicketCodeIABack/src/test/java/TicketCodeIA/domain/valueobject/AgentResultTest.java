package TicketCodeIA.domain.valueobject;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentResultTest {

    @Test
    void success_createsSuccessResult() {
        AgentResult result = AgentResult.success("ok");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isFailure()).isFalse();
        assertThat(result.needsChanges()).isFalse();
        assertThat(result.getMessage()).isEqualTo("ok");
    }

    @Test
    void failure_createsFailureResult() {
        AgentResult result = AgentResult.failure("error");
        assertThat(result.isFailure()).isTrue();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.needsChanges()).isFalse();
    }

    @Test
    void needsChanges_createsNeedsChangesResult() {
        AgentResult result = AgentResult.needsChanges("fix this");
        assertThat(result.needsChanges()).isTrue();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.isFailure()).isFalse();
    }
}
