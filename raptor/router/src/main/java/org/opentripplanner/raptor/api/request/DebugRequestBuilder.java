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
  private Consumer<DebugEvent<ArrivalView<?>>> destinationArrivalListener;
  private DebugLogger logger;

  DebugRequestBuilder(DebugRequest debug) {
    this.stops.addAll(debug.stops());
    this.path.addAll(debug.path());
    this.debugPathFromStopIndex = debug.debugPathFromStopIndex();
    this.stopArrivalListener = debug.stopArrivalListener();
    this.patternRideDebugListener = debug.patternRideDebugListener();
    this.destinationArrivalListener = debug.destinationArrivalListener();
    this.logger = debug.logger();
  }

  public DebugRequestBuilder withStops(Collection<Integer> stops) {
    this.stops.addAll(stops);
    return this;
  }

  public DebugRequestBuilder withStops(int... stops) {
    return withStops(Arrays.stream(stops).boxed().collect(Collectors.toList()));
  }

  public DebugRequestBuilder withPath(List<Integer> stopsInPath) {
    if (!path.isEmpty()) {
      throw new IllegalStateException(
        "The API support only one debug path. " + "Existing: " + path + ", new: " + stopsInPath
      );
    }
    this.path = new ArrayList<>(stopsInPath);
    return this;
  }

  /**
   * Select the stop index where the debugging should start. It is the index of the stops in the
   * path list.
   */
  public DebugRequestBuilder withDebugPathFromStopIndex(int stopIndex) {
    this.debugPathFromStopIndex = stopIndex;
    return this;
  }

  public DebugRequestBuilder withStopArrivalListener(
    Consumer<DebugEvent<ArrivalView<?>>> listener
  ) {
    this.stopArrivalListener = listener;
    return this;
  }

  public DebugRequestBuilder withPatternRideDebugListener(
    Consumer<DebugEvent<PatternRideView<?, ?>>> listener
  ) {
    this.patternRideDebugListener = listener;
    return this;
  }

  public DebugRequestBuilder withDestinationArrivalListener(
    Consumer<DebugEvent<ArrivalView<?>>> listener
  ) {
    this.destinationArrivalListener = listener;
    return this;
  }

  public DebugRequestBuilder withLogger(DebugLogger logger) {
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
      destinationArrivalListener,
      logger
    );
  }
}
