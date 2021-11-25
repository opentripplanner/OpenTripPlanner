package org.opentripplanner.transit.raptor.rangeraptor.debug;

import org.opentripplanner.transit.raptor.api.request.DebugRequest;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.rangeraptor.WorkerLifeCycle;
import org.opentripplanner.transit.raptor.rangeraptor.multicriteria.PatternRide;

import java.util.LinkedList;

final class DebugHandlerPatternRideAdapter extends AbstractDebugHandlerAdapter<PatternRide<?>> {

    DebugHandlerPatternRideAdapter(DebugRequest debug, WorkerLifeCycle lifeCycle) {
        super(debug, debug.patternRideDebugListener(), lifeCycle);
    }

    @Override
    protected int stop(PatternRide<?> ride) {
        return ride.boardStopIndex;
    }

    @Override
    protected Iterable<Integer> stopsVisited(PatternRide<?> ride) {
        return listStopsForDebugging(ride.prevArrival);
    }

    /**
     * List all stops used to arrive at current stop arrival. This method can be SLOW,
     * should only be used in code that does not need to be fast, like debugging.
     */
    private Iterable<Integer> listStopsForDebugging(ArrivalView<?> it) {
        LinkedList<Integer> stops = new LinkedList<>();

        while (!it.arrivedByAccess()) {
            stops.addFirst(it.stop());
            it = it.previous();
        }
        stops.addFirst(it.stop());

        return stops;
    }
}
