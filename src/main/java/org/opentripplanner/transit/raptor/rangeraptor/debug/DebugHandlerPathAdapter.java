package org.opentripplanner.transit.raptor.rangeraptor.debug;

import java.util.List;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.request.DebugRequest;
import org.opentripplanner.transit.raptor.rangeraptor.WorkerLifeCycle;

final class DebugHandlerPathAdapter extends AbstractDebugHandlerAdapter<Path<?>> {

  DebugHandlerPathAdapter(DebugRequest debug, WorkerLifeCycle lifeCycle) {
    super(debug, debug.pathFilteringListener(), lifeCycle);
  }

  @Override
  protected int stop(Path<?> path) {
    return path.egressLeg().fromStop();
  }

  @Override
  protected List<Integer> stopsVisited(Path<?> path) {
    return path.listStops();
  }
}
