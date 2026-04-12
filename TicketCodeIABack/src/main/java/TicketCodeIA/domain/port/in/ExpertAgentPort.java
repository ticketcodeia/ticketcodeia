package TicketCodeIA.domain.port.in;

import java.util.List;
import java.util.Map;

public interface ExpertAgentPort {

    /**
     * The Expert Agent chooses which TODO ticket should be processed next.
     * It considers dependencies, priorities, and logical build order.
     */
    Long chooseNextTicket(List<Map<String, String>> allTicketsSummary, List<Map<String, Object>> todoTickets);
}
