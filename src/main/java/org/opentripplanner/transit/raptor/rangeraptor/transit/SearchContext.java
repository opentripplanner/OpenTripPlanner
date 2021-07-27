package org.opentripplanner.transit.raptor.rangeraptor.transit;

import static org.opentripplanner.transit.raptor.rangeraptor.transit.SlackProviderAdapter.forwardSlackProvider;
import static org.opentripplanner.transit.raptor.rangeraptor.transit.SlackProviderAdapter.reverseSlackProvider;

import java.util.Collection;
import java.util.function.ToIntFunction;
import org.opentripplanner.transit.raptor.api.debug.DebugLogger;
import org.opentripplanner.transit.raptor.api.request.DebugRequest;
import org.opentripplanner.transit.raptor.api.request.McCostParams;
import org.opentripplanner.transit.raptor.api.request.RaptorProfile;
import org.opentripplanner.transit.raptor.api.request.RaptorRequest;
import org.opentripplanner.transit.raptor.api.request.RaptorTuningParameters;
import org.opentripplanner.transit.raptor.api.request.SearchParams;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.DefaultCostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransitDataProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.rangeraptor.RoundProvider;
import org.opentripplanner.transit.raptor.rangeraptor.SlackProvider;
import org.opentripplanner.transit.raptor.rangeraptor.WorkerLifeCycle;
import org.opentripplanner.transit.raptor.rangeraptor.debug.DebugHandlerFactory;
import org.opentripplanner.transit.raptor.rangeraptor.debug.WorkerPerformanceTimers;
import org.opentripplanner.transit.raptor.rangeraptor.path.ForwardPathMapper;
import org.opentripplanner.transit.raptor.rangeraptor.path.PathMapper;
import org.opentripplanner.transit.raptor.rangeraptor.path.ReversePathMapper;
import org.opentripplanner.transit.raptor.rangeraptor.workerlifecycle.LifeCycleEventPublisher;
import org.opentripplanner.transit.raptor.rangeraptor.workerlifecycle.LifeCycleSubscriptions;

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
    protected final RaptorTransitDataProvider<T> transit;

    private final TransitCalculator<T> calculator;
    private final CostCalculator<T> costCalculator;
    private final RaptorTuningParameters tuningParameters;
    private final RoundTracker roundTracker;
    private final PathMapper<T> pathMapper;
    private final WorkerPerformanceTimers timers;
    private final DebugHandlerFactory<T> debugFactory;

    private final LifeCycleSubscriptions lifeCycleSubscriptions = new LifeCycleSubscriptions();

    public SearchContext(
            RaptorRequest<T> request,
            RaptorTuningParameters tuningParameters,
            RaptorTransitDataProvider<T> transit,
            WorkerPerformanceTimers timers
    ) {
        this.request = request;
        this.tuningParameters = tuningParameters;
        this.transit = transit;
        // Note that it is the "new" request that is passed in.
        this.calculator = createCalculator(this.request, tuningParameters);
        this.costCalculator = createCostCalculator(
            transit.stopBoarAlightCost(),
            request.multiCriteriaCostFactors()
        );
        this.roundTracker = new RoundTracker(
            nRounds(),
            request.searchParams().numberOfAdditionalTransfers(),
            lifeCycle()
        );
        this.pathMapper = createPathMapper(request, lifeCycle());
        this.timers = timers;
        this.debugFactory = new DebugHandlerFactory<>(debugRequest(request), lifeCycle());
    }

    public Collection<RaptorTransfer> accessPaths() {
        return request.searchDirection().isForward()
                ? request.searchParams().accessPaths()
                : request.searchParams().egressPaths();
    }

    public Collection<RaptorTransfer> egressPaths() {
        return request.searchDirection().isForward()
                ? request.searchParams().egressPaths()
                : request.searchParams().accessPaths();
    }

    public int[] egressStops() {
        return egressPaths().stream().mapToInt(RaptorTransfer::stop).toArray();
    }

    public SearchParams searchParams() {
        return request.searchParams();
    }

    public RaptorProfile profile() {
        return request.profile();
    }

    public RaptorTransitDataProvider<T> transit() {
        return transit;
    }

    public TransitCalculator<T> calculator() {
        return calculator;
    }

    /**
     * Create new slack-provider for use in Raptor, handles reverse and forward
     * search as well as including transfer-slack into board-slack between transits.
     * <p>
     * The {@code SlackProvider} is stateful, so this method create a new instance
     * every time it is called, so each consumer could have their own instance and
     * not get surprised by the life-cycle update. Remember to call the
     * {@link SlackProvider#setCurrentPattern(RaptorTripPattern)} before retriving
     * slack values.
     */
    public SlackProvider slackProvider() {
        return createSlackProvider(request, lifeCycle());
    }

    /**
     * The board-slack (duration time in seconds) to add to the stop arrival time,
     * before boarding the given trip pattern. THIS DO NOT INCLUDE THE transfer-slack,
     * and should only be used to time-shift the access-path.
     * <p>
     * Unit: seconds.
     */
    public ToIntFunction<RaptorTripPattern> boardSlackProvider() {
        return createBoardSlackProvider(request);
    }

    public PathMapper<T> pathMapper() {
        return pathMapper;
    }

    public CostCalculator<T> costCalculator() {
        return costCalculator;
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

    public RoundProvider roundProvider() {
        return roundTracker;
    }

    public WorkerLifeCycle lifeCycle() {
        return lifeCycleSubscriptions;
    }

    public LifeCycleEventPublisher createLifeCyclePublisher() {
        LifeCycleEventPublisher publisher = new LifeCycleEventPublisher(lifeCycleSubscriptions);
        // We want the code to fail if someone try to attach to the worker lifecycle
        // after it is initialized; Hence close for new subscriptions
        lifeCycleSubscriptions.close();
        return publisher;
    }

    public boolean enableGuaranteedTransfers() {
        if(profile().isOneOf(RaptorProfile.BEST_TIME, RaptorProfile.NO_WAIT_BEST_TIME)) {
            return false;
        }
        return searchParams().guaranteedTransfersEnabled();
    }

    // TODO(transfers) double check we want to skip the same profile
    public boolean enableForbiddenTransfers() {
        if(profile().isOneOf(RaptorProfile.BEST_TIME, RaptorProfile.NO_WAIT_BEST_TIME)) {
            return false;
        }
        return searchParams().forbiddenTransfersEnabled();
    }

    /* private methods */

    /**
     * Create a new calculator depending on the desired search direction.
     */
    private static <T extends RaptorTripSchedule> TransitCalculator<T> createCalculator(
            RaptorRequest<T> r, RaptorTuningParameters t
    ) {
        SearchParams s = r.searchParams();
        return r.searchDirection().isForward()
                ? new ForwardTransitCalculator<>(s, t)
                : new ReverseTransitCalculator<>(s, t);
    }

    private static DebugRequest debugRequest(
            RaptorRequest<?> request
    ) {
        return request.searchDirection().isForward()
                ? request.debug()
                : request.mutate().debug().reverseDebugRequest().build();
    }

    private static SlackProvider createSlackProvider(
            RaptorRequest<?> request,
            WorkerLifeCycle lifeCycle
    ) {
        return request.searchDirection().isForward()
                ? forwardSlackProvider(request.slackProvider(), lifeCycle)
                : reverseSlackProvider(request.slackProvider(), lifeCycle);
    }

    private static ToIntFunction<RaptorTripPattern> createBoardSlackProvider(
            RaptorRequest<?> request
    ) {
        return request.searchDirection().isForward()
                ? p -> request.slackProvider().boardSlack(p)
                : p -> request.slackProvider().alightSlack(p);
    }

    private static <S extends RaptorTripSchedule> PathMapper<S> createPathMapper(
            RaptorRequest<S> request,
            WorkerLifeCycle lifeCycle
    ) {
        return request.searchDirection().isForward()
                ? new ForwardPathMapper<>(request.slackProvider(), lifeCycle)
                : new ReversePathMapper<>(request.slackProvider(), lifeCycle);
    }

    private CostCalculator<T> createCostCalculator(int[] stopVisitCost, McCostParams f) {
        return new DefaultCostCalculator<T>(
                f.boardCost(),
                f.transferCost(),
                f.waitReluctanceFactor(),
                stopVisitCost,
                f.transitReluctanceFactors()
        );
    }
}
