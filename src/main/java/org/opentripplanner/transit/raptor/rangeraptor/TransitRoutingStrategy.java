package org.opentripplanner.transit.raptor.rangeraptor;

import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TripScheduleSearch;


/**
 * Provides alternative implementations of some transit-specific logic within the {@link
 * RangeRaptorWorker}.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public interface TransitRoutingStrategy<T extends RaptorTripSchedule> {

    /**
     * Prepare the {@link TransitRoutingStrategy} to route using the given pattern and tripSearch.
     */
    void prepareForTransitWith(RaptorTripPattern pattern, TripScheduleSearch<T> tripSearch);

    /**
     * Perform the routing with the initialized pattern and tripSearch at the given
     * stopPositionInPattern.
     * <p/>
     * This method is called for each stop position in a pattern after the first stop reached in the
     * previous round.
     *
     * @param stopPositionInPattern the current stop position in the pattern set in {@link
     *                              #prepareForTransitWith(RaptorTripPattern, TripScheduleSearch)}
     */
    void routeTransitAtStop(final int stopPositionInPattern);
}
