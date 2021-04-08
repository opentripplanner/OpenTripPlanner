package org.opentripplanner.transit.raptor.api.request;

import org.opentripplanner.transit.raptor.api.debug.DebugEvent;
import org.opentripplanner.transit.raptor.api.debug.DebugLogger;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.rangeraptor.multicriteria.PatternRide;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;


/**
 * This class configure the amount of debugging you want for your request.
 * Debugging is supported by an event model and event listeners must be provided to
 * receive any debug info.
 * <p/>
 * To debug unexpected results is sometimes very time consuming. This class make it possible
 * to list all stop arrival events during the search for a given list of stops and/or a path.
 * <p/>
 * The debug events are not returned as part of the result, instead they are posted
 * to registered listeners. The events are temporary objects; hence you should not
 * hold a reference to the event elements or to any part of it after the listener callback
 * completes.
 * <p/>
 * One of the benefits of the event based return strategy is that the events are returned
 * even in the case of an exception or entering a endless loop. You don´t need to wait
 * for the result to start analyze the results.
 *
 * <h3>Debugging stops</h3>
 * By providing a small set of stops to debug, a list of all events for those stops
 * are returned. This can be useful both to understand the algorithm and to debug events
 * at a particular stop.
 *
 * <h3>Debugging path</h3>
 * To debug a path(or trip), provide the list of stops and a index. You will then only get
 * events for that particular sequence of stops starting with the stop at the given index.
 * This is very effect if you expect a trip and don´t get it. Most likely you will get a
 * REJECT or DROP event for your trip in return. You will also get a list of tips dominating
 * the particular trip.
 */
public class DebugRequest {

    /**
     * Return a debug request with defaults values.
     */
    static <T extends RaptorTripSchedule> DebugRequest defaults() {
        return new DebugRequest();
    }

    private final List<Integer> stops;
    private final List<Integer> path;
    private final int debugPathFromStopIndex;
    private final Consumer<DebugEvent<ArrivalView<?>>> stopArrivalListener;
    private final Consumer<DebugEvent<PatternRide<?>>> patternRideDebugListener;
    private final Consumer<DebugEvent<Path<?>>> pathFilteringListener;
    private final DebugLogger logger;

    private DebugRequest() {
        stops = Collections.emptyList();
        path = Collections.emptyList();
        debugPathFromStopIndex = 0;
        stopArrivalListener = null;
        patternRideDebugListener = null;
        pathFilteringListener = null;
        logger = (topic, message) -> {};
    }

    DebugRequest(DebugRequestBuilder builder) {
        this.stops = Collections.unmodifiableList(builder.stops());
        this.path = Collections.unmodifiableList(builder.path());
        this.debugPathFromStopIndex = builder.debugPathFromStopIndex();
        this.stopArrivalListener = builder.stopArrivalListener();
        this.patternRideDebugListener = builder.patternRideDebugListener();
        this.pathFilteringListener = builder.pathFilteringListener();
        this.logger = builder.logger();
    }


    /**
     * List of stops to debug.
     */
    public List<Integer> stops() {
        return stops;
    }

    /**
     * List of stops in a particular path to debug. Only one path can be debugged per request.
     */
    public List<Integer> path() {
        return path;
    }

    /**
     * The first stop to start recording debug information in the path specified in this request.
     * This will filter away all events in the beginning of the path reducing the number of events
     * significantly; Hence make it easier to inspect events towards the end of the trip.
     */
    public int debugPathFromStopIndex() {
        return debugPathFromStopIndex;
    }

    public Consumer<DebugEvent<ArrivalView<?>>> stopArrivalListener() {
        return stopArrivalListener;
    }

    public Consumer<DebugEvent<PatternRide<?>>> patternRideDebugListener() {
        return patternRideDebugListener;
    }

    public Consumer<DebugEvent<Path<?>>> pathFilteringListener() {
        return pathFilteringListener;
    }

    public DebugLogger logger() {
        return logger;
    }

    @Override
    public String toString() {
        return "DebugRequest{" +
                "stops=" + stops +
                ", path=" + path +
                ", startAtStopIndex=" + debugPathFromStopIndex +
                ", stopArrivalListener=" + enabled(stopArrivalListener) +
                ", pathFilteringListener=" + enabled(pathFilteringListener) +
                ", logger=" + enabled(logger) +
                '}';
    }

    private static String enabled(Object obj) {
        return obj == null ? "null" : "enabled";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        DebugRequest that = (DebugRequest) o;
        return debugPathFromStopIndex == that.debugPathFromStopIndex &&
                Objects.equals(stops, that.stops) &&
                Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stops, path, debugPathFromStopIndex);
    }
}
