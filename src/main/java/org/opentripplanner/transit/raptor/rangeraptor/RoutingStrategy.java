package org.opentripplanner.transit.raptor.rangeraptor;

import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TripScheduleSearch;


/**
 * Provides alternative implementations of some logic within the {@link RangeRaptorWorker}.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public interface RoutingStrategy<T extends RaptorTripSchedule> {

    /**
     * Sets the access time for the departure stop. This method is called for each access path
     * in every Raptor iteration. The access path can have more than one "leg"; hence the
     * implementation need to be aware of the round (Walk access in round 0, Flex with one leg
     * in round 1, ...).
     *
     * @param iterationDepartureTime The current iteration departure time.
     * @param timeDependentDepartureTime The access might be restricted to a given time window,
     *                                   if so this is the time shifted to fit the window.
     */
    void setAccessToStop(
        RaptorTransfer accessPath,
        int iterationDepartureTime,
        int timeDependentDepartureTime
    );

    /**
     * Prepare the {@link RoutingStrategy} to route using the given pattern and tripSearch.
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
