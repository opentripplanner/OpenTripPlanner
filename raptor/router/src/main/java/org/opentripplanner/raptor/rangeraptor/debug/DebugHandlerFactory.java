package org.opentripplanner.raptor.rangeraptor.debug;

import java.util.function.Function;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.debug.DebugLogger;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.api.request.DebugRequest;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.api.view.PatternRideView;
import org.opentripplanner.raptor.rangeraptor.internalapi.DebugHandler;
import org.opentripplanner.raptor.rangeraptor.internalapi.WorkerLifeCycle;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.util.paretoset.ParetoSetEventListener;

/**
 * Use this factory to create debug handlers. If a routing request has not enabled debugging {@code
 * null} is returned. Use the {@link #isDebugStopArrival(int)} like methods before retrieving a
 * handler.
 * <p>
 * See the <b>package.md</b> for Debugging implementation notes in the raptor root package {@link
 * org.opentripplanner.transit}.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class DebugHandlerFactory<T extends RaptorTripSchedule> {

  private final DebugHandler<ArrivalView<?>> stopHandler;
  private final DebugHandler<RaptorPath<?>> pathHandler;
  private final DebugHandler<PatternRideView<?, ?>> patternRideHandler;
  private final DebugLogger logger;

  public DebugHandlerFactory(DebugRequest request, WorkerLifeCycle lifeCycle) {
    this.stopHandler = isDebug(request.stopArrivalListener())
      ? new DebugHandlerStopArrivalAdapter(request, lifeCycle)
      : null;

    this.pathHandler = isDebug(request.pathFilteringListener())
      ? new DebugHandlerPathAdapter(request, lifeCycle)
      : null;

    this.patternRideHandler = isDebug(request.patternRideDebugListener())
      ? new DebugHandlerPatternRideAdapter(request, lifeCycle)
      : null;

    this.logger = request.logger();
    lifeCycle.onRouteSearch(logger::setSearchDirection);
  }

  /* Stop Arrival */

  public boolean isDebugStopArrival() {
    return isDebug(stopHandler);
  }

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

  /* PatternRide */

  @Nullable
  public ParetoSetEventListener<PatternRideView<?, ?>> paretoSetPatternRideListener() {
    return patternRideHandler == null
      ? null
      : new ParetoSetDebugHandlerAdapter<>(patternRideHandler);
  }

  /* path */

  /**
   * Create a pareto set listener for a set of elements that can be mapped to a path, like
   * destination arrivals. The path is only created if path debugging is enabled, and the events
   * are reported to the path debug listener.
   *
   * @param toPath maps an element to a path, or to {@code null} if the element should not be
   *               reported
   */
  @Nullable
  public <E> ParetoSetEventListener<E> paretoSetDebugPathListener(
    Function<E, RaptorPath<?>> toPath
  ) {
    return pathHandler == null ? null : new ParetoSetDebugPathAdapter<>(pathHandler, toPath);
  }

  @Nullable
  public DebugHandler<RaptorPath<?>> debugPathArrival() {
    return pathHandler;
  }

  /* logger */

  public DebugLogger debugLogger() {
    return logger;
  }

  /* private methods */

  private boolean isDebug(Object handler) {
    return handler != null;
  }
}
