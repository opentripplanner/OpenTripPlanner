package org.opentripplanner.raptor.rangeraptor.debug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opentripplanner.raptor.api.request.DebugRequest;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.api.view.PatternRideView;
import org.opentripplanner.raptor.rangeraptor.internalapi.WorkerLifeCycle;

final class DebugHandlerPatternRideAdapter
  extends AbstractDebugHandlerAdapter<PatternRideView<?, ?>>
{

  DebugHandlerPatternRideAdapter(DebugRequest debug, WorkerLifeCycle lifeCycle) {
    super(debug, debug.patternRideDebugListener(), lifeCycle);
  }

  @Override
  protected int stopIndex(PatternRideView<?, ?> ride) {
    return ride.boardStopIndex();
  }

  @Override
  protected Iterable<Integer> stopsVisited(PatternRideView<?, ?> ride) {
    return listStopsForDebugging(ride.prevArrival());
  }

  /**
   * List all stops used to arrive at current stop arrival. This method can be SLOW, should only be
   * used in code that does not need to be fast, like debugging.
   */
  private Iterable<Integer> listStopsForDebugging(ArrivalView<?> it) {
    List<Integer> stops = new ArrayList<>();

    while (it != null) {
      stops.add(it.stop());
      it = it.previous();
    }
    Collections.reverse(stops);
    return stops;
  }
}
