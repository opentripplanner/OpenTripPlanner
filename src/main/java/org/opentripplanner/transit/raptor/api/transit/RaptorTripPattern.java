package org.opentripplanner.transit.raptor.api.transit;


/**
 * This interface represent a trip pattern. A trip-pattern in the raptor context is
 * just a list of stops visited by ALL trips in the pattern. The stops must be ordered
 * in the same sequence, with no gaps, as the trips visit the stops.
 */
public interface RaptorTripPattern {

    /**
     * The stop index
     * @param stopPositionInPattern stop position number in pattern, starting at 0.
     */
    int stopIndex(int stopPositionInPattern);

    /**
     * Number of stops in pattern.
     */
    int numberOfStopsInPattern();
}
