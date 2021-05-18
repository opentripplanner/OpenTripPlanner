package org.opentripplanner.transit.raptor.rangeraptor;

import java.util.function.IntConsumer;
import java.util.function.ToIntFunction;
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
    void alight(final int stopIndex, final int stopPos, ToIntFunction<T> getStopArrivalTime);

    /**
     * Board trip for each stopArrival (Std have only one "best" arrival, while Mc may have many).
     */
    void forEachBoarding(int stopIndex, IntConsumer prevStopArrivalTimeConsumer);

    /**
     * Get the current boarding previous transit arrival. This is used to look up any
     * guaranteed transfers.
     */
    TransitArrival<T> previousTransit(int boardStopIndex);
    void board(
            final int stopIndex,
            final int earliestBoardTime,
            RaptorTripScheduleBoardOrAlightEvent<T> result
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
}
