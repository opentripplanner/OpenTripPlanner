package org.opentripplanner.transit.raptor.service;

import org.opentripplanner.transit.raptor.api.request.RaptorRequest;
import org.opentripplanner.transit.raptor.rangeraptor.debug.WorkerPerformanceTimers;
import org.opentripplanner.transit.raptor.util.AvgTimer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * This is a cash of performance timers. There is a timer pr request "type". The
 * {@link RequestAlias#alias(RaptorRequest, boolean)} is used to categorize requests
 * and each category get its own timer.
 * <p>
 * The timer creation is lazy initialized, hence need to be thread-safe.
 */
public class WorkerPerformanceTimersCache {

    /**
     * This map need to be THREAD-SAFE (ConcurrentHashMap) because of the lacy initialization
     * of the instances based on the concurrent requests.
     */
    private final Map<String, WorkerPerformanceTimers> timers = new ConcurrentHashMap<>();
    private final boolean multiThreaded;

    public WorkerPerformanceTimersCache(boolean multiThreaded) {
        this.multiThreaded = multiThreaded;
    }

    public WorkerPerformanceTimers get(RaptorRequest<?> request) {
        if(AvgTimer.timersEnabled()) {
            return timers.computeIfAbsent(RequestAlias.alias(request, multiThreaded),
                WorkerPerformanceTimers::new
            );
        }
        else {
            return WorkerPerformanceTimers.NOOP;
        }
    }
}
