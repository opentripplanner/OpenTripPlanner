package org.opentripplanner.transit.raptor.api.request;

import org.opentripplanner.transit.raptor.api.debug.DebugEvent;
import org.opentripplanner.transit.raptor.api.debug.DebugLogger;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.rangeraptor.multicriteria.PatternRide;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Mutable version of {@link DebugRequest}.
 */
public class DebugRequestBuilder {
    private final List<Integer> stops = new ArrayList<>();
    private final List<Integer> path = new ArrayList<>();
    private int debugPathFromStopIndex;
    private Consumer<DebugEvent<ArrivalView<?>>> stopArrivalListener;
    private Consumer<DebugEvent<PatternRide<?>>> patternRideDebugListener;
    private Consumer<DebugEvent<Path<?>>> pathFilteringListener;
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


    public List<Integer> stops() {
        return stops;
    }

    public DebugRequestBuilder addStops(Collection<Integer> stops) {
        this.stops.addAll(stops);
        return this;
    }

    public DebugRequestBuilder addStops(int ... stops) {
        return addStops(Arrays.stream(stops).boxed().collect(Collectors.toList()));
    }

    public List<Integer> path() {
        return path;
    }

    public DebugRequestBuilder addPath(Collection<Integer> path) {
        if(!path.isEmpty()) {
            throw new IllegalStateException("The API support only one debug path. Existing: " + this.path + ", new: " + path);
        }
        this.path.addAll(path);
        return this;
    }

    public int debugPathFromStopIndex() {
        return debugPathFromStopIndex;
    }

    public DebugRequestBuilder debugPathFromStopIndex(Integer debugPathStartAtStopIndex) {
        this.debugPathFromStopIndex = debugPathStartAtStopIndex;
        return this;
    }

    public Consumer<DebugEvent<ArrivalView<?>>> stopArrivalListener() {
        return stopArrivalListener;
    }

    public DebugRequestBuilder stopArrivalListener(Consumer<DebugEvent<ArrivalView<?>>> listener) {
        this.stopArrivalListener = listener;
        return this;
    }

    public Consumer<DebugEvent<PatternRide<?>>> patternRideDebugListener() {
        return patternRideDebugListener;
    }

    public DebugRequestBuilder patternRideDebugListener(Consumer<DebugEvent<PatternRide<?>>> listener) {
        this.patternRideDebugListener = listener;
        return this;
    }

    public Consumer<DebugEvent<Path<?>>> pathFilteringListener() {
        return pathFilteringListener;
    }

    public DebugRequestBuilder pathFilteringListener(Consumer<DebugEvent<Path<?>>> listener) {
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
        return new DebugRequest(this);
    }
}
