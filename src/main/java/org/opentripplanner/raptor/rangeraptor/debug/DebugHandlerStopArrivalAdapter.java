package org.opentripplanner.raptor.rangeraptor.debug;

import java.util.LinkedList;
import org.opentripplanner.raptor.api.request.DebugRequest;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.rangeraptor.internalapi.WorkerLifeCycle;

final class DebugHandlerStopArrivalAdapter extends AbstractDebugHandlerAdapter<ArrivalView<?>> {

  DebugHandlerStopArrivalAdapter(DebugRequest debug, WorkerLifeCycle lifeCycle) {
    super(debug, debug.stopArrivalListener(), lifeCycle);
  }

  @Override
  protected int stop(ArrivalView<?> arrival) {
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
    LinkedList<Integer> stops = new LinkedList<>();

    while (!it.arrivedByAccess()) {
      stops.addFirst(it.stop());
      it = it.previous();
    }
    stops.addFirst(it.stop());

    return stops;
  }
}
