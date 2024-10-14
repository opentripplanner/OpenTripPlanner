package org.opentripplanner.routing.api.response;

/**
 * This represents an admissible connection between two via segments.
 *
 * @param from Index of the itinerary for this segment
 * @param to   Index of the itinerary for the next segment
 */
public record ViaRoutingResponseConnection(int from, int to) {}
