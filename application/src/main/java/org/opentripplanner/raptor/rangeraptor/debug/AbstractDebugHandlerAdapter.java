package org.opentripplanner.raptor.rangeraptor.debug;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import org.opentripplanner.raptor.api.debug.DebugEvent;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.request.DebugRequest;
import org.opentripplanner.raptor.rangeraptor.internalapi.DebugHandler;
import org.opentripplanner.raptor.rangeraptor.internalapi.WorkerLifeCycle;

/**
 * Generic abstract implementation of the {@link DebugHandler} interface. The main purpose is to
 * provide a common logic for the adapters between the Range Raptor domain and the outside client -
 * the event listeners and the request API.
 *
 * @param <T> The element type for the debug event
 */
abstract class AbstractDebugHandlerAdapter<T> implements DebugHandler<T> {

  private final List<Integer> stops;
  private final List<Integer> path;
  private final int pathStartAtStopIndex;
  private final Consumer<DebugEvent<T>> eventListener;
  private int iterationDepartureTime = -1;

  AbstractDebugHandlerAdapter(
    DebugRequest debugRequest,
    Consumer<DebugEvent<T>> eventListener,
    WorkerLifeCycle lifeCycle
  ) {
    this.stops = debugRequest.stops();
    this.path = debugRequest.path();
    this.pathStartAtStopIndex = debugRequest.debugPathFromStopIndex();
    this.eventListener = eventListener;

    // Attach debugger to RR life cycle to receive iteration setup events
    lifeCycle.onSetupIteration(this::setupIteration);
  }

  @Override
  public boolean isDebug(int stop) {
    return stops.contains(stop) || isDebugTrip(stop);
  }

  @Override
  public void accept(T element) {
    // The "if" is needed because this is the first time we are able to check trip paths
    if (isDebugStopOrTripPath(element)) {
      eventListener.accept(DebugEvent.accept(iterationDepartureTime, element));
    }
  }

  @Override
  public void reject(T element, T rejectedByElement, String reason) {
    // The "if" is needed because this is the first time we are able to check trip paths
    if (isDebugStopOrTripPath(element)) {
      eventListener.accept(
        DebugEvent.reject(iterationDepartureTime, element, rejectedByElement, reason)
      );
    }
  }

  @Override
  public void drop(T element, T droppedByElement, String reason) {
    // The "if" is needed because this is the first time we are able to check trip paths
    if (isDebugStopOrTripPath(element)) {
      eventListener.accept(
        DebugEvent.drop(iterationDepartureTime, element, droppedByElement, reason)
      );
    }
  }

  /**
   * Returns {@link RaptorConstants#NOT_FOUND} not supported.
   */
  protected abstract int stop(T arrival);

  protected abstract Iterable<Integer> stopsVisited(T arrival);

  /* private members */

  private void setupIteration(int iterationDepartureTime) {
    this.iterationDepartureTime = iterationDepartureTime;
  }

  /**
   * Check if a stop exist among the trip path stops which should be debugged.
   */
  private boolean isDebugTrip(int stop) {
    return pathStartAtStopIndex <= path.indexOf(stop);
  }

  private boolean isDebugStopOrTripPath(T arrival) {
    return stops.contains(stop(arrival)) || isDebugTripPath(arrival);
  }

  private boolean isDebugTripPath(T arrival) {
    if (!isDebugTrip(stop(arrival))) {
      return false;
    }

    Iterator<Integer> stopsVisited = stopsVisited(arrival).iterator();
    Iterator<Integer> pathStops = path.iterator();

    while (stopsVisited.hasNext()) {
      if (!pathStops.hasNext()) {
        return false;
      }
      Integer visitedStop = stopsVisited.next();
      Integer pathStop = pathStops.next();

      if (!visitedStop.equals(pathStop)) {
        return false;
      }
    }
    return true;
  }
}
