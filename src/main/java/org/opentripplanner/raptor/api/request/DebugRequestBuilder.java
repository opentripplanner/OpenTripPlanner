package org.opentripplanner.raptor.api.request;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.opentripplanner.raptor.api.debug.DebugEvent;
import org.opentripplanner.raptor.api.debug.DebugLogger;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.api.view.PatternRideView;

/**
 * Mutable version of {@link DebugRequest}.
 */
public class DebugRequestBuilder {

  private final Set<Integer> stops = new HashSet<>();
  private List<Integer> path = new ArrayList<>();
  private int debugPathFromStopIndex;
  private Consumer<DebugEvent<ArrivalView<?>>> stopArrivalListener;
  private Consumer<DebugEvent<PatternRideView<?, ?>>> patternRideDebugListener;
  private Consumer<DebugEvent<RaptorPath<?>>> pathFilteringListener;
  private DebugLogger logger;

  DebugRequestBuilder(DebugRequest debug) {
    this.stops.addAll(debug.stops());
    this.path.addAll(debug.path());
    this.debugPathFromStopIndex = debug.debugPathFromStopIndex();
    this.stopArrivalListener = debug.stopArrivalListener();
    this.patternRideDebugListener = debug.patternRideDebugListener();
    this.pathFilteringListener = debug.pathFilteringListener();
    this.logger = debug.logger();
  }

  /** Read-only view to stops added sorted in ascending order. */
  public List<Integer> stops() {
    return stops.stream().sorted().collect(Collectors.toList());
  }

  public DebugRequestBuilder addStops(Collection<Integer> stops) {
    this.stops.addAll(stops);
    return this;
  }

  public DebugRequestBuilder addStops(int... stops) {
    return addStops(Arrays.stream(stops).boxed().collect(Collectors.toList()));
  }

  /**
   * The list of stops for a given path to debug.
   */
  public List<Integer> path() {
    return path;
  }

  public DebugRequestBuilder setPath(List<Integer> stopsInPath) {
    if (!path.isEmpty()) {
      throw new IllegalStateException(
        "The API support only one debug path. " + "Existing: " + path + ", new: " + stopsInPath
      );
    }
    this.path = new ArrayList<>(stopsInPath);
    return this;
  }

  public int debugPathFromStopIndex() {
    return debugPathFromStopIndex;
  }

  /**
   * Select the stop index where the debugging should start. It is the index of the stops in the
   * path list.
   */
  public DebugRequestBuilder debugPathFromStopIndex(int stopIndex) {
    this.debugPathFromStopIndex = stopIndex;
    return this;
  }

  public Consumer<DebugEvent<ArrivalView<?>>> stopArrivalListener() {
    return stopArrivalListener;
  }

  public DebugRequestBuilder stopArrivalListener(Consumer<DebugEvent<ArrivalView<?>>> listener) {
    this.stopArrivalListener = listener;
    return this;
  }

  public Consumer<DebugEvent<PatternRideView<?, ?>>> patternRideDebugListener() {
    return patternRideDebugListener;
  }

  public DebugRequestBuilder patternRideDebugListener(
    Consumer<DebugEvent<PatternRideView<?, ?>>> listener
  ) {
    this.patternRideDebugListener = listener;
    return this;
  }

  public Consumer<DebugEvent<RaptorPath<?>>> pathFilteringListener() {
    return pathFilteringListener;
  }

  public DebugRequestBuilder pathFilteringListener(Consumer<DebugEvent<RaptorPath<?>>> listener) {
    this.pathFilteringListener = listener;
    return this;
  }

  public DebugLogger logger() {
    return logger;
  }

  public DebugRequestBuilder logger(DebugLogger logger) {
    this.logger = logger;
    return this;
  }

  public DebugRequestBuilder reverseDebugRequest() {
    Collections.reverse(this.path);
    return this;
  }

  public DebugRequest build() {
    return new DebugRequest(
      List.copyOf(stops),
      List.copyOf(path),
      debugPathFromStopIndex,
      stopArrivalListener,
      patternRideDebugListener,
      pathFilteringListener,
      logger
    );
  }
}
