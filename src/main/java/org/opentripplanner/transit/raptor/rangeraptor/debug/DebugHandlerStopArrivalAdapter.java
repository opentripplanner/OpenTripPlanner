package org.opentripplanner.transit.raptor.rangeraptor.debug;

import org.opentripplanner.transit.raptor.api.request.DebugRequest;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.rangeraptor.WorkerLifeCycle;

import java.util.LinkedList;

/**
 * StopArrival adapter.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
final class DebugHandlerStopArrivalAdapter<T extends RaptorTripSchedule>
        extends AbstractDebugHandlerAdapter<ArrivalView<T>> {

    DebugHandlerStopArrivalAdapter(DebugRequest<T> debug, WorkerLifeCycle lifeCycle) {
        super(debug, debug.stopArrivalListener(), lifeCycle);
    }

    @Override
    protected int stop(ArrivalView<T> arrival) {
        return arrival.stop();
    }

    @Override
    protected Iterable<Integer> stopsVisited(ArrivalView<T> arrival) {
        return listStopsForDebugging(arrival);
    }

    /**
     * List all stops used to arrive at current stop arrival. This method can be SLOW,
     * should only be used in code that does not need to be fast, like debugging.
     */
    private Iterable<Integer> listStopsForDebugging(ArrivalView<T> it) {
        LinkedList<Integer> stops = new LinkedList<>();

        while (!it.arrivedByAccess()) {
            stops.addFirst(it.stop());
            it = it.previous();
        }
        stops.addFirst(it.stop());

        return stops;
    }
}
