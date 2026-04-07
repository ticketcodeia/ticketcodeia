package TicketCodeIA.agent;

import TicketCodeIA.entity.Ticket;
import TicketCodeIA.enums.Priority;
import TicketCodeIA.enums.TicketStatus;
import TicketCodeIA.service.AgentLogService;
import TicketCodeIA.service.SseService;
import TicketCodeIA.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentOrchestratorTest {

    @Mock private TicketService ticketService;
    @Mock private DeveloperAgent developerAgent;
    @Mock private ReviewerAgent reviewerAgent;
    @Mock private TesterAgent testerAgent;
    @Mock private AgentLogService agentLogService;
    @Mock private SseService sseService;

    @InjectMocks
    private AgentOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orchestrator, "maxRetries", 3);
        // Lenient stubs shared across all tests
        lenient().when(agentLogService.log(anyLong(), any(), anyString(), anyString())).thenReturn(null);
        lenient().doNothing().when(sseService).broadcast(any());
        lenient().when(ticketService.saveTicket(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Ticket ticket(Long id, TicketStatus status) {
        return Ticket.builder()
                .id(id)
                .title("Ticket " + id)
                .description("Desc")
                .status(status)
                .priority(Priority.MEDIUM)
                .build();
    }

    // ── Already in final state → skip ─────────────────────────────────────────

    @Test
    void processTicket_alreadyDone_skipsProcessing() {
        when(ticketService.getTicketEntity(1L)).thenReturn(ticket(1L, TicketStatus.DONE));

        orchestrator.processTicket(1L, true, true);

        verify(developerAgent, never()).process(any());
        verify(reviewerAgent, never()).process(any());
        verify(testerAgent, never()).process(any());
    }

    @Test
    void processTicket_alreadyEscalated_skipsProcessing() {
        when(ticketService.getTicketEntity(1L)).thenReturn(ticket(1L, TicketStatus.ESCALATED));

        orchestrator.processTicket(1L, true, true);

        verify(developerAgent, never()).process(any());
    }

    // ── Dev-only (both agents disabled) ───────────────────────────────────────

    @Test
    void processTicket_bothDisabled_marksAsDoneAfterDev() {
        // Calls: initial, loop-start, after-dev → markDone → return
        Ticket todo     = ticket(1L, TicketStatus.TODO);
        Ticket afterDev = ticket(1L, TicketStatus.CODE_REVIEW);

        when(ticketService.getTicketEntity(1L)).thenReturn(todo, todo, afterDev);
        when(developerAgent.process(any())).thenReturn(AgentResult.success("done"));

        orchestrator.processTicket(1L, false, false);

        verify(developerAgent).process(any());
        verify(reviewerAgent, never()).process(any());
        verify(testerAgent, never()).process(any());
        // afterDev is mutated to DONE inside markDone
        verify(ticketService, atLeastOnce()).saveTicket(
                argThat((Ticket t) -> t.getStatus() == TicketStatus.DONE));
    }

    // ── Dev + Review only (testing disabled) ──────────────────────────────────

    @Test
    void processTicket_reviewOnly_skipsTestingAfterApproval() {
        // Calls: initial, loop-start, after-dev, end-of-loop,
        //        loop-start(CODE_REVIEW), inside-approved(getEntity), → markDone → return
        Ticket todo       = ticket(1L, TicketStatus.TODO);
        Ticket codeReview = ticket(1L, TicketStatus.CODE_REVIEW);

        when(ticketService.getTicketEntity(1L))
                .thenReturn(todo, todo, codeReview, codeReview, codeReview, codeReview);
        when(developerAgent.process(any())).thenReturn(AgentResult.success("implemented"));
        when(reviewerAgent.process(any())).thenReturn(AgentResult.success("approved"));

        orchestrator.processTicket(1L, true, false);

        verify(developerAgent).process(any());
        verify(reviewerAgent).process(any());
        verify(testerAgent, never()).process(any());
        verify(ticketService, atLeastOnce()).saveTicket(
                argThat((Ticket t) -> t.getStatus() == TicketStatus.DONE));
    }

    // ── Dev + Testing only (review disabled) ──────────────────────────────────

    @Test
    void processTicket_testingOnly_skipsBranchToTesting() {
        // Calls: initial, loop-start, after-dev(→set TESTING), end-of-loop,
        //        loop-start(TESTING) → tester-success → return
        Ticket todo    = ticket(1L, TicketStatus.TODO);
        Ticket testing = ticket(1L, TicketStatus.TESTING);

        when(ticketService.getTicketEntity(1L))
                .thenReturn(todo, todo, testing, testing, testing);
        when(developerAgent.process(any())).thenReturn(AgentResult.success("implemented"));
        when(testerAgent.process(any())).thenReturn(AgentResult.success("all tests passed"));

        orchestrator.processTicket(1L, false, true);

        verify(developerAgent).process(any());
        verify(reviewerAgent, never()).process(any());
        verify(testerAgent).process(any());
    }

    // ── Full pipeline (both enabled) ──────────────────────────────────────────

    @Test
    void processTicket_fullPipeline_runsAllAgents() {
        // Calls: initial, loop-start(TODO), after-dev, end-of-loop,
        //        loop-start(CODE_REVIEW), end-of-loop,
        //        loop-start(TESTING) → tester-success → return
        Ticket todo       = ticket(1L, TicketStatus.TODO);
        Ticket codeReview = ticket(1L, TicketStatus.CODE_REVIEW);
        Ticket testing    = ticket(1L, TicketStatus.TESTING);

        when(ticketService.getTicketEntity(1L))
                .thenReturn(todo, todo, codeReview, codeReview, codeReview, testing, testing);
        when(developerAgent.process(any())).thenReturn(AgentResult.success("implemented"));
        when(reviewerAgent.process(any())).thenReturn(AgentResult.success("approved"));
        when(testerAgent.process(any())).thenReturn(AgentResult.success("tests passed"));

        orchestrator.processTicket(1L, true, true);

        verify(developerAgent).process(any());
        verify(reviewerAgent).process(any());
        verify(testerAgent).process(any());
    }

    // ── Dev failure → escalation ──────────────────────────────────────────────

    @Test
    void processTicket_devFailure_escalatesTicket() {
        // Calls: initial, loop-start → dev-failure → escalate → return
        Ticket todo = ticket(1L, TicketStatus.TODO);

        when(ticketService.getTicketEntity(1L)).thenReturn(todo, todo);
        when(developerAgent.process(any())).thenReturn(AgentResult.failure("compilation error"));

        orchestrator.processTicket(1L, true, true);

        verify(reviewerAgent, never()).process(any());
        verify(testerAgent, never()).process(any());
        verify(ticketService, atLeastOnce()).saveTicket(
                argThat((Ticket t) -> t.getStatus() == TicketStatus.ESCALATED));
    }

    // ── Reviewer failure → escalation ────────────────────────────────────────

    @Test
    void processTicket_reviewerFailure_escalatesTicket() {
        // Calls: initial, loop-start(TODO), after-dev, end-of-loop,
        //        loop-start(CODE_REVIEW) → reviewer-failure → escalate → return
        Ticket todo       = ticket(1L, TicketStatus.TODO);
        Ticket codeReview = ticket(1L, TicketStatus.CODE_REVIEW);

        when(ticketService.getTicketEntity(1L))
                .thenReturn(todo, todo, codeReview, codeReview, codeReview);
        when(developerAgent.process(any())).thenReturn(AgentResult.success("done"));
        when(reviewerAgent.process(any())).thenReturn(AgentResult.failure("review crashed"));

        orchestrator.processTicket(1L, true, true);

        verify(testerAgent, never()).process(any());
        verify(ticketService, atLeastOnce()).saveTicket(
                argThat((Ticket t) -> t.getStatus() == TicketStatus.ESCALATED));
    }

    // ── Reviewer requests changes → retry ─────────────────────────────────────

    @Test
    void processTicket_reviewerRequestsChanges_retriesDevelopment() {
        // Round 1: todo→dev→codeReview→reviewer(needs-changes)→inProgress
        // Round 2: inProgress→dev→codeReview→reviewer(approved)→testing→tester(pass)
        Ticket todo       = ticket(1L, TicketStatus.TODO);
        Ticket codeReview = ticket(1L, TicketStatus.CODE_REVIEW);
        Ticket inProgress = ticket(1L, TicketStatus.IN_PROGRESS);
        Ticket testing    = ticket(1L, TicketStatus.TESTING);

        // 13 calls:
        // init(todo), loop(todo), afterDev(codeReview), endLoop(codeReview),
        // loop(codeReview), needsChanges(inProgress), endLoop(inProgress),
        // loop(inProgress), afterDev(codeReview), endLoop(codeReview),
        // loop(codeReview), endLoop(testing),
        // loop(testing)
        when(ticketService.getTicketEntity(1L)).thenReturn(
                todo, todo, codeReview, codeReview,
                codeReview, inProgress, inProgress,
                inProgress, codeReview, codeReview,
                codeReview, testing, testing);
        when(developerAgent.process(any())).thenReturn(AgentResult.success("done"));
        when(reviewerAgent.process(any()))
                .thenReturn(AgentResult.needsChanges("add tests"))
                .thenReturn(AgentResult.success("approved"));
        when(testerAgent.process(any())).thenReturn(AgentResult.success("all pass"));

        orchestrator.processTicket(1L, true, true);

        verify(developerAgent, times(2)).process(any());
        verify(reviewerAgent, times(2)).process(any());
        verify(testerAgent).process(any());
    }

    // ── Max retries exceeded → escalation ────────────────────────────────────

    @Test
    void processTicket_maxRetriesExceeded_escalates() {
        // maxRetries=3: reviewer requests changes 3 times → 3rd triggers escalation
        // After 3rd needsChanges++ (developmentCycles=3 >= 3), escalate directly (no extra getEntity)
        Ticket todo       = ticket(1L, TicketStatus.TODO);
        Ticket codeReview = ticket(1L, TicketStatus.CODE_REVIEW);
        Ticket inProgress = ticket(1L, TicketStatus.IN_PROGRESS);

        // 17 calls (round 3 has no inside-needsChanges since escalation happens first)
        when(ticketService.getTicketEntity(1L)).thenReturn(
                todo,
                todo, codeReview, codeReview, codeReview, inProgress, inProgress,     // round 1
                inProgress, codeReview, codeReview, codeReview, inProgress, inProgress, // round 2
                inProgress, codeReview, codeReview, codeReview);                       // round 3 (escalate)
        when(developerAgent.process(any())).thenReturn(AgentResult.success("done"));
        when(reviewerAgent.process(any())).thenReturn(AgentResult.needsChanges("still issues"));

        orchestrator.processTicket(1L, true, false);

        verify(ticketService, atLeastOnce()).saveTicket(
                argThat((Ticket t) -> t.getStatus() == TicketStatus.ESCALATED));
    }

    // ── Tester failure → retry ────────────────────────────────────────────────

    @Test
    void processTicket_testerFailure_retriesDevelopment() {
        // Round 1: todo→dev→testing(setByOrchestrator)→tester(fail)→inProgress
        // Round 2: inProgress→dev→testing→tester(pass)
        Ticket todo       = ticket(1L, TicketStatus.TODO);
        Ticket testing    = ticket(1L, TicketStatus.TESTING);
        Ticket inProgress = ticket(1L, TicketStatus.IN_PROGRESS);

        // 11 calls
        when(ticketService.getTicketEntity(1L)).thenReturn(
                todo, todo, testing, testing, testing, inProgress, inProgress,
                inProgress, testing, testing, testing);
        when(developerAgent.process(any())).thenReturn(AgentResult.success("done"));
        when(testerAgent.process(any()))
                .thenReturn(AgentResult.failure("tests failed"))
                .thenReturn(AgentResult.success("tests passed"));

        orchestrator.processTicket(1L, false, true);

        verify(developerAgent, times(2)).process(any());
        verify(testerAgent, times(2)).process(any());
        verify(reviewerAgent, never()).process(any());
    }

    // ── Async wrapper ─────────────────────────────────────────────────────────

    @Test
    void processTicketAsync_callsProcessTicket() {
        when(ticketService.getTicketEntity(1L)).thenReturn(ticket(1L, TicketStatus.DONE));

        orchestrator.processTicketAsync(1L, true, true);

        verify(ticketService).getTicketEntity(1L);
    }
}
