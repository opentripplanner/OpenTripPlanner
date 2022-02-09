package org.opentripplanner.transit.raptor.rangeraptor;

import java.util.function.IntConsumer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripScheduleBoardOrAlightEvent;
import org.opentripplanner.transit.raptor.api.transit.TransitArrival;


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
    void prepareForTransitWith(RaptorTripPattern pattern);

    /**
     * Alight the current trip at the given stop with the arrival times.
     */
    void alight(final int stopIndex, final int stopPos, final int alightSlack);

    /**
     * Board trip for each stopArrival (Std have only one "best" arrival, while Mc may have many).
     */
    void forEachBoarding(int stopIndex, IntConsumer prevStopArrivalTimeConsumer);

    /**
     * Get the current boarding previous transit arrival. This is used to look up any
     * guaranteed transfers.
     */
    TransitArrival<T> previousTransit(int boardStopIndex);

    /**
     * Board the given trip(event) at the given stop index.
     * @param earliestBoardTime used to calculate wait-time (if needed)
     */
    void board(
            final int stopIndex,
            final int earliestBoardTime,
            RaptorTripScheduleBoardOrAlightEvent<T> boarding
    );

    /**
     * The trip search will use this index to search relative to an existing boarding.
     * This make a subsequent search faster since it must board an earlier trip, and the
     * trip search can start at the given onTripIndex.
     * if not the current trip is used.
     * <p>
     * Return -1 to if the tripIndex is unknown.
     */
    default int onTripIndex() { return -1; }

    /**
     * This method allow the strategy to replace the existing boarding (if it exists) with
     * a better option. It is left to the implementation to check that a boarding already exist.
     *
     * @param earliestBoardTime - the earliest possible time a boarding can take place
     * @param stopPos - the pattern stop position
     * @param stopIndex - the global stop index
     */
    default void boardSameTrip(int earliestBoardTime, int stopPos, int stopIndex) {
        // Do nothing. For standard and multi-criteria Raptor we do not need to do anything.
    }
}
