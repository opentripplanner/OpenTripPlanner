package org.opentripplanner.transit.raptor.api.transit;


/**
 * The purpose of this interface is to provide information about the
 * trip schedule. The trip is a child of, and lives in the context
 * of a trip pattern.
 * <p/>
 * The purpose of hiding these attributes behind an interface is to
 * allow the implementation to chose the most efficient underlying
 * implementation that suits its needs.
 */
public interface RaptorTripSchedule {

    /**
     * The arrival time at the given stop position in pattern.
     * @param stopPosInPattern the stop position.
     * @return the arrival time in seconds at the given stop
     */
    int arrival(int stopPosInPattern);

    /**
     * The departure time at the given stop position in pattern.
     * @param stopPosInPattern the stop position.
     * @return the arrival time in seconds at the given stop
     */
    int departure(int stopPosInPattern);

    /**
     * The implementation should provide a very short description with enough information to identify
     * the the trip. This is used for debugging and logging.
     * <p/>
     * In a GTFS world this would be <em>pattern index/id</em> and <em>trip index</em>. Use something like this:
     * <p/>
     * {@code "P-5_T-12"}
     */
    String debugInfo();


    /**
     * Return the pattern for this trip.
     */
    RaptorTripPattern pattern();
}
