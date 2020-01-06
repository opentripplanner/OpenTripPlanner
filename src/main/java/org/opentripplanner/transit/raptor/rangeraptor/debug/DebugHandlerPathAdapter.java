package org.opentripplanner.transit.raptor.rangeraptor.debug;

import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.request.DebugRequest;
import org.opentripplanner.transit.raptor.api.transit.TripScheduleInfo;
import org.opentripplanner.transit.raptor.rangeraptor.WorkerLifeCycle;

import java.util.List;

/**
 * Path adapter.
 *
 * @param <T> The TripSchedule type defined by the user of the range raptor API.
 */
final class DebugHandlerPathAdapter <T extends TripScheduleInfo> extends AbstractDebugHandlerAdapter<Path<T>> {

    DebugHandlerPathAdapter(DebugRequest<T> debug, WorkerLifeCycle lifeCycle) {
        super(debug, debug.pathFilteringListener(), lifeCycle);
    }

    @Override
    protected int stop(Path<T> path) {
        return path.egressLeg().fromStop();
    }

    @Override
    protected List<Integer> stopsVisited(Path<T> path) {
        return path.listStops();
    }
}
