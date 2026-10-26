package org.opentripplanner.raptor.rangeraptor.debug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opentripplanner.raptor.api.request.DebugRequest;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.rangeraptor.internalapi.WorkerLifeCycle;

final class DebugHandlerStopArrivalAdapter extends AbstractDebugHandlerAdapter<ArrivalView<?>> {

  DebugHandlerStopArrivalAdapter(DebugRequest debug, WorkerLifeCycle lifeCycle) {
    super(debug, debug.stopArrivalListener(), lifeCycle);
  }

  @Override
  protected int stopIndex(ArrivalView<?> arrival) {
    return arrival.stop();
  }

  @Override
  protected Iterable<Integer> stopsVisited(ArrivalView<?> arrival) {
    return listStopsForDebugging(arrival);
  }

  /**
   * List all stops used to arrive at current stop arrival. This method can be SLOW, should only be
   * used in code that does not need to be fast, like debugging.
   */
  private Iterable<Integer> listStopsForDebugging(ArrivalView<?> it) {
    List<Integer> stops = new ArrayList<>();

    // loop until access is done(previous is null)
    while (it != null) {
      stops.add(it.stop());
      it = it.previous();
    }
    Collections.reverse(stops);
    return stops;
  }
}
