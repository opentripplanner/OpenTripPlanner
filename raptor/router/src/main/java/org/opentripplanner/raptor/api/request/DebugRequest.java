package org.opentripplanner.raptor.api.request;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.opentripplanner.raptor.api.debug.DebugEvent;
import org.opentripplanner.raptor.api.debug.DebugLogger;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.api.view.PatternRideView;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * This class configure the amount of debugging you want for your request. Debugging is supported by
 * an event model and event listeners must be provided to receive any debug info.
 * <p/>
 * To debug unexpected results is sometimes very time-consuming. This class make it possible to list
 * all stop arrival events during the search for a given list of stops and/or a path.
 * <p/>
 * The debug events are not returned as part of the result, instead they are posted to registered
 * listeners. The events are temporary objects; hence you should not hold a reference to the event
 * elements or to any part of it after the listener callback completes.
 * <p/>
 * One of the benefits of the event based return strategy is that the events are returned even in
 * the case of an exception or entering a endless loop. You don´t need to wait for the result to
 * start analyze the results.
 *
 * <h3>Debugging stops</h3>
 * By providing a small set of stops to debug, a list of all events for those stops are returned.
 * This can be useful both to understand the algorithm and to debug events at a particular stop.
 *
 * <h3>Debugging path</h3>
 * To debug a path(or trip), provide the list of stops and a index. You will then only get events
 * for that particular sequence of stops starting with the stop at the given index. This is very
 * effect if you expect a trip and don´t get it. Most likely you will get a REJECT or DROP event for
 * your trip in return. You will also get a list of tips dominating the particular trip.
 */
public record DebugRequest(
  /** List of stops to debug, unordered. */
  List<Integer> stops,
  /** List of stops in a particular path to debug. Only one path can be debugged per request. */
  List<Integer> path,
  /**
   * The first stop to start recording debug information in the path specified in this request.
   * This will filter away all events in the beginning of the path reducing the number of events
   * significantly; Hence make it easier to inspect events towards the end of the trip.
   */
  int debugPathFromStopIndex,
  Consumer<DebugEvent<ArrivalView<?>>> stopArrivalListener,
  Consumer<DebugEvent<PatternRideView<?, ?>>> patternRideDebugListener,
  Consumer<DebugEvent<RaptorPath<?>>> pathFilteringListener,
  DebugLogger logger
) {
  private static final DebugRequest DEFAULT_DEBUG_REQUEST = new DebugRequest(
    List.of(),
    List.of(),
    0,
    null,
    null,
    null,
    DebugLogger.noop()
  );

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DebugRequest that = (DebugRequest) o;
    return (
      debugPathFromStopIndex == that.debugPathFromStopIndex &&
      Objects.equals(stops, that.stops) &&
      Objects.equals(path, that.path)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(stops, path, debugPathFromStopIndex);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(DebugRequest.class)
      .addCol("stops", stops)
      .addCol("path", path)
      .addNum("startAtStopIndex", debugPathFromStopIndex, 0)
      .addBoolIfTrue("stopArrivalListener", stopArrivalListener != null)
      .addBoolIfTrue("pathFilteringListener", pathFilteringListener != null)
      .addBoolIfTrue("logger", logger != DEFAULT_DEBUG_REQUEST.logger())
      .toString();
  }

  /**
   * Return a debug request with defaults values.
   */
  static DebugRequest defaults() {
    return DEFAULT_DEBUG_REQUEST;
  }
}
