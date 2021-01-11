package org.opentripplanner.transit.raptor.rangeraptor.debug;

import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.request.DebugRequest;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.rangeraptor.WorkerLifeCycle;
import org.opentripplanner.transit.raptor.rangeraptor.view.DebugHandler;
import org.opentripplanner.transit.raptor.util.paretoset.ParetoSetEventListener;

import javax.annotation.Nullable;


/**
 * Use this factory to create debug handlers. If a routing request has not enabled debugging
 * {@code null} is returned. Use the {@link #isDebugStopArrival(int)} like methods before
 * retrieving a handler.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class DebugHandlerFactory<T extends RaptorTripSchedule> {
  private final DebugHandler<ArrivalView<?>> stopHandler;
  private final DebugHandler<Path<?>> pathHandler;

  public DebugHandlerFactory(DebugRequest request, WorkerLifeCycle lifeCycle) {
    this.stopHandler = isDebug(request.stopArrivalListener())
        ? new DebugHandlerStopArrivalAdapter(request, lifeCycle)
        : null;

    this.pathHandler = isDebug(request.pathFilteringListener())
        ? new DebugHandlerPathAdapter(request, lifeCycle)
        : null;
  }

  /* Stop Arrival */

  public DebugHandler<ArrivalView<?>> debugStopArrival() {
    return stopHandler;
  }

  @Nullable
  public ParetoSetEventListener<ArrivalView<T>> paretoSetStopArrivalListener(int stop) {
    return isDebugStopArrival(stop) ? new ParetoSetDebugHandlerAdapter<>(stopHandler) : null;
  }

  public boolean isDebugStopArrival(int stop) {
    return stopHandler != null && stopHandler.isDebug(stop);
  }


  /* path */

  @Nullable
  public ParetoSetDebugHandlerAdapter<Path<?>> paretoSetDebugPathListener() {
    return pathHandler == null ? null : new ParetoSetDebugHandlerAdapter<>(pathHandler);
  }


  /* private methods */

  private boolean isDebug(Object handler) {
    return handler != null;
  }

  public boolean isDebugStopArrival() {
    return stopHandler != null;
  }
}
