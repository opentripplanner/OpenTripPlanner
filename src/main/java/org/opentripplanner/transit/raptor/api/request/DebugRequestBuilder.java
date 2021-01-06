package org.opentripplanner.transit.raptor.api.request;

import org.opentripplanner.transit.raptor.api.debug.DebugEvent;
import org.opentripplanner.transit.raptor.api.debug.DebugLogger;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Mutable version of {@link DebugRequest}.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class DebugRequestBuilder<T extends RaptorTripSchedule> {
    private final List<Integer> stops = new ArrayList<>();
    private final List<Integer> path = new ArrayList<>();
    private int debugPathFromStopIndex;
    private Consumer<DebugEvent<ArrivalView<T>>> stopArrivalListener;
    private Consumer<DebugEvent<Path<T>>> pathFilteringListener;
    private DebugLogger logger;


    DebugRequestBuilder(DebugRequest<T> debug) {
        this.stops.addAll(debug.stops());
        this.path.addAll(debug.path());
        this.debugPathFromStopIndex = debug.debugPathFromStopIndex();
        this.stopArrivalListener = debug.stopArrivalListener();
        this.pathFilteringListener = debug.pathFilteringListener();
        this.logger = debug.logger();
    }


    public List<Integer> stops() {
        return stops;
    }

    public DebugRequestBuilder<T> addStops(Collection<Integer> stops) {
        this.stops.addAll(stops);
        return this;
    }

    public DebugRequestBuilder<T> addStops(int ... stops) {
        return addStops(Arrays.stream(stops).boxed().collect(Collectors.toList()));
    }

    public List<Integer> path() {
        return path;
    }

    public DebugRequestBuilder<T> addPath(Collection<Integer> path) {
        if(!path.isEmpty()) {
            throw new IllegalStateException("The API support only one debug path. Existing: " + this.path + ", new: " + path);
        }
        this.path.addAll(path);
        return this;
    }

    public int debugPathFromStopIndex() {
        return debugPathFromStopIndex;
    }

    public DebugRequestBuilder<T> debugPathFromStopIndex(Integer debugPathStartAtStopIndex) {
        this.debugPathFromStopIndex = debugPathStartAtStopIndex;
        return this;
    }

    public Consumer<DebugEvent<ArrivalView<T>>> stopArrivalListener() {
        return stopArrivalListener;
    }

    public DebugRequestBuilder<T> stopArrivalListener(Consumer<DebugEvent<ArrivalView<T>>> stopArrivalListener) {
        this.stopArrivalListener = stopArrivalListener;
        return this;
    }

    public Consumer<DebugEvent<Path<T>>> pathFilteringListener() {
        return pathFilteringListener;
    }

    public DebugRequestBuilder<T> pathFilteringListener(Consumer<DebugEvent<Path<T>>> pathFilteringListener) {
        this.pathFilteringListener = pathFilteringListener;
        return this;
    }

    public DebugLogger logger() {
        return logger;
    }

    public DebugRequestBuilder<T> logger(DebugLogger logger) {
        this.logger = logger;
        return this;
    }

    public DebugRequestBuilder<T> reverseDebugRequest() {
        Collections.reverse(this.path);
        return this;
    }

    public DebugRequest<T> build() {
        return new DebugRequest<>(this);
    }

}
