package TicketCodeIA.domain.port.in;

import java.util.List;
import java.util.Map;

public interface POAgentPort {
    List<Map<String, String>> generateTicketData(String requirements);
}
