package org.opentripplanner.raptor.rangeraptor.debug;

import java.util.List;
import org.opentripplanner.raptor.api.request.DebugRequest;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.rangeraptor.internalapi.WorkerLifeCycle;
import org.opentripplanner.raptor.spi.RaptorConstants;

/**
 * Debug adapter for destination arrivals. The arrival is identified by the last stop arrival
 * before the egress. An arrival without a stop arrival chain, like the probe used for
 * qualification checks, is not part of the search and is never reported.
 */
final class DebugHandlerDestinationArrivalAdapter
  extends AbstractDebugHandlerAdapter<ArrivalView<?>>
{

  DebugHandlerDestinationArrivalAdapter(DebugRequest debug, WorkerLifeCycle lifeCycle) {
    super(debug, debug.destinationArrivalListener(), lifeCycle);
  }

  @Override
  protected int stopIndex(ArrivalView<?> arrival) {
    var last = arrival.previous();
    return last == null ? RaptorConstants.NOT_FOUND : last.stop();
  }

  @Override
  protected Iterable<Integer> stopsVisited(ArrivalView<?> arrival) {
    var last = arrival.previous();
    return last == null ? List.of() : DebugHandlerStopArrivalAdapter.listStops(last);
  }
}
