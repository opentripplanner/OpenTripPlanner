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
     * Return {@code true} if it is allowed/possible to board at a particular stop index. This
     * should include checks like: Does the pattern allow boarding at the given stop? Is this
     * accessible to wheelchairs (if requested).
     *
     * @param stopPositionInPattern stop position number in pattern, starting at 0.
     */
    boolean boardingPossibleAt(int stopPositionInPattern);


    /**
     * Same as {@link #boardingPossibleAt(int)}, but for getting off a trip.
     * @param stopPositionInPattern stop position number in pattern, starting at 0.
     */
    boolean alightingPossibleAt(int stopPositionInPattern);

    /**
     * Number of stops in pattern.
     */
    int numberOfStopsInPattern();

    /**
     * Pattern debug info, return transit mode and route name. This is  used for debugging purposes
     * only. The implementation should provide a short description with enough information for
     * humans to identify the the trip/route. This is is used in a context where information about
     * agency and stop is known, so there is no need to include agency or geographical region
     * information.
     * <p/>
     * The recommended string to return is: {@code [MODE] [SHORT_ROUTE_DESCRIPTION]}.
     */
    String debugInfo();
}
