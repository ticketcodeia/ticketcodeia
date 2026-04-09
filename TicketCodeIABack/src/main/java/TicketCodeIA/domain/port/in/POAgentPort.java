package TicketCodeIA.domain.port.in;

import java.util.List;
import java.util.Map;

public interface POAgentPort {
    List<Map<String, String>> generateTicketData(String requirements);

    /**
     * Given all tickets with their titles/statuses and the list of TODO ticket IDs+titles,
     * the PO agent decides which TODO ticket should be processed next.
     * Returns the ID of the chosen ticket.
     */
    Long chooseNextTicket(List<Map<String, String>> allTicketsSummary, List<Map<String, Object>> todoTickets);
}
