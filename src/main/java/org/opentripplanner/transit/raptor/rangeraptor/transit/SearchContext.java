package org.opentripplanner.transit.raptor.rangeraptor.transit;

import org.opentripplanner.transit.raptor.api.debug.DebugLogger;
import org.opentripplanner.transit.raptor.api.request.DebugRequest;
import org.opentripplanner.transit.raptor.api.request.McCostParams;
import org.opentripplanner.transit.raptor.api.request.RaptorProfile;
import org.opentripplanner.transit.raptor.api.request.RaptorRequest;
import org.opentripplanner.transit.raptor.api.request.SearchParams;
import org.opentripplanner.transit.raptor.api.request.RaptorTuningParameters;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.TransitDataProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.rangeraptor.RoundProvider;
import org.opentripplanner.transit.raptor.rangeraptor.WorkerLifeCycle;
import org.opentripplanner.transit.raptor.rangeraptor.debug.DebugHandlerFactory;
import org.opentripplanner.transit.raptor.rangeraptor.debug.WorkerPerformanceTimers;
import org.opentripplanner.transit.raptor.rangeraptor.workerlifecycle.LifeCycleSubscriptions;
import org.opentripplanner.transit.raptor.rangeraptor.workerlifecycle.LifeCycleEventPublisher;

import java.util.Collection;

/**
 * The search context is used to hold search scoped instances and to pass these
 * to who ever need them.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class SearchContext<T extends RaptorTripSchedule> {
    private static final DebugLogger NOOP_DEBUG_LOGGER = (topic, message) -> { };
    /**
     * The request input used to customize the worker to the clients needs.
     */
    private final RaptorRequest<T> request;

    /**
     * the transit data role needed for routing
     */
    protected final TransitDataProvider<T> transit;

    private final TransitCalculator calculator;
    private final RaptorTuningParameters tuningParameters;
    private final RoundTracker roundTracker;
    private final WorkerPerformanceTimers timers;
    private final DebugHandlerFactory<T> debugFactory;

    private LifeCycleSubscriptions lifeCycleSubscriptions = new LifeCycleSubscriptions();

    public SearchContext(
            RaptorRequest<T> request,
            RaptorTuningParameters tuningParameters,
            TransitDataProvider<T> transit,
            WorkerPerformanceTimers timers
    ) {
        this.request = request;
        this.tuningParameters = tuningParameters;
        this.transit = transit;
        // Note that it is the "new" request that is passed in.
        this.calculator = createCalculator(this.request, tuningParameters);
        this.roundTracker = new RoundTracker(nRounds(), request.searchParams().numberOfAdditionalTransfers(), lifeCycle());
        this.timers = timers;
        this.debugFactory = new DebugHandlerFactory<>(debugRequest(request), lifeCycle());
    }

    public Collection<RaptorTransfer> accessLegs() {
        return request.searchDirection().isForward()
                ? request.searchParams().accessLegs()
                : request.searchParams().egressLegs();
    }

    public Collection<RaptorTransfer> egressLegs() {
        return request.searchDirection().isForward()
                ? request.searchParams().egressLegs()
                : request.searchParams().accessLegs();
    }

    public int[] egressStops() {
        return egressLegs().stream().mapToInt(RaptorTransfer::stop).toArray();
    }

    public SearchParams searchParams() {
        return request.searchParams();
    }

    public RaptorProfile profile() {
        return request.profile();
    }

    public RaptorTuningParameters tuningParameters() {
        return tuningParameters;
    }

    public TransitDataProvider<T> transit() {
        return transit;
    }

    public TransitCalculator calculator() {
        return calculator;
    }

    public CostCalculator costCalculator() {
        McCostParams f = request.multiCriteriaCostFactors();
        return new CostCalculator(
                f.boardCost(),
                request.searchParams().boardSlackInSeconds(),
                f.walkReluctanceFactor(),
                f.waitReluctanceFactor(),
                lifeCycle()
        );
    }

    public WorkerPerformanceTimers timers() {
        return timers;
    }

    public DebugHandlerFactory<T> debugFactory() {
        return debugFactory;
    }

    public DebugLogger debugLogger() {
        DebugLogger logger = request.debug().logger();
        return logger != null ? logger : NOOP_DEBUG_LOGGER;
    }

    /** Number of stops in transit graph. */
    public int nStops() {
        return transit.numberOfStops();
    }

    /** Calculate the maximum number of rounds to perform. */
    public int nRounds() {
        if(request.searchParams().isMaxNumberOfTransfersSet()) {
            return request.searchParams().maxNumberOfTransfers() + 1;
        }
        return tuningParameters.maxNumberOfTransfers() + 1;
    }

    /**
     * Create a new calculator depending on the desired search direction.
     */
    private static TransitCalculator createCalculator(RaptorRequest<?> r, RaptorTuningParameters t) {
        SearchParams s = r.searchParams();
        return r.searchDirection().isForward()
                ? new ForwardSearchTransitCalculator(s, t)
                : new ReverseSearchTransitCalculator(s, t);
    }

    private DebugRequest<T> debugRequest(RaptorRequest<T> request) {
        return request.searchDirection().isForward()
                ? request.debug()
                : request.mutate().debug().reverseDebugRequest().build();
    }

    public RoundProvider roundProvider() {
        return roundTracker;
    }

    public WorkerLifeCycle lifeCycle() {
        return lifeCycleSubscriptions;
    }

    public LifeCycleEventPublisher createLifeCyclePublisher() {
        LifeCycleEventPublisher publisher = new LifeCycleEventPublisher(lifeCycleSubscriptions);
        // We want the code to fail (NPE) if someone try to attach to the worker workerlifecycle
        // after it is iniziated; Hence set the builder to null:
        lifeCycleSubscriptions = null;
        return publisher;
    }
}
