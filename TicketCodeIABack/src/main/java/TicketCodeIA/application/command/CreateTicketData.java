package TicketCodeIA.application.command;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data for a single ticket to create, used by the Expert Agent's createTickets tool.
 */
public record CreateTicketData(
        @JsonProperty("title") String title,
        @JsonProperty("description") String description,
        @JsonProperty("priority") String priority,
        @JsonProperty("enableCodeReview") boolean enableCodeReview,
        @JsonProperty("enableTesting") boolean enableTesting) {}
