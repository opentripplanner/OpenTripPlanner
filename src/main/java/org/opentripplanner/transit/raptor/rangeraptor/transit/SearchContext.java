package org.opentripplanner.transit.raptor.rangeraptor.transit;

import org.opentripplanner.transit.raptor.api.request.*;
import org.opentripplanner.transit.raptor.api.transit.*;
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

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

import static org.opentripplanner.transit.raptor.rangeraptor.transit.SlackProviderAdapter.forwardSlackProvider;
import static org.opentripplanner.transit.raptor.rangeraptor.transit.SlackProviderAdapter.reverseSlackProvider;

/**
 * The search context is used to hold search scoped instances and to pass these
 * to who ever need them.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class SearchContext<T extends RaptorTripSchedule> {
    /**
     * The request input used to customize the worker to the clients needs.
     */
    private final RaptorRequest<T> request;

    /**
     * the transit data role needed for routing
     */
    protected final RaptorTransitDataProvider<T> transit;

    private final TransitCalculator<T> calculator;
    private final CostCalculator costCalculator;
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
        this.costCalculator = request.profile().is(RaptorProfile.MULTI_CRITERIA)
                ? transit.multiCriteriaCostCalculator() : null;
        this.roundTracker = new RoundTracker(
                nRounds(),
                request.searchParams().numberOfAdditionalTransfers(),
                lifeCycle()
        );
        this.pathMapper = createPathMapper(
                this.transit.transferConstraintsSearch(),
                this.costCalculator,
                transit.stopNameResolver(),
                request,
                lifeCycle()
        );
        this.timers = timers;
        this.debugFactory = new DebugHandlerFactory<>(debugRequest(request), lifeCycle());
    }

    public Collection<RaptorTransfer> accessPaths() {
        return accessOrEgressPaths(
                request.searchDirection().isForward(),
                profile(),
                request.searchParams()
        );

    }

    public Collection<RaptorTransfer> egressPaths() {
        return accessOrEgressPaths(
                request.searchDirection().isInReverse(),
                profile(),
                request.searchParams()
        );
    }

    public int[] egressStops() {
        return egressPaths().stream()
                .mapToInt(RaptorTransfer::stop)
                .distinct()
                .toArray();
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

    @Nullable
    public CostCalculator costCalculator() {
        return costCalculator;
    }

    public WorkerPerformanceTimers timers() {
        return timers;
    }

    public DebugHandlerFactory<T> debugFactory() {
        return debugFactory;
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

    public boolean enableConstrainedTransfers() {
        return searchParams().constrainedTransfersEnabled();
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
            RaptorPathConstrainedTransferSearch<S> txConstraintsSearch,
            CostCalculator costCalculator,
            RaptorStopNameResolver stopNameResolver,
            RaptorRequest<S> request,
            WorkerLifeCycle lifeCycle
    ) {
        return request.searchDirection().isForward()
                ? new ForwardPathMapper<>(
                        txConstraintsSearch,
                        request.slackProvider(),
                        costCalculator,
                        stopNameResolver,
                        lifeCycle,
                        request.profile().useApproximateTripSearch()
                )
                : new ReversePathMapper<>(
                        txConstraintsSearch,
                        request.slackProvider(),
                        costCalculator,
                        stopNameResolver,
                        lifeCycle,
                        request.profile().useApproximateTripSearch()
                );
    }

    public RaptorStopNameResolver stopNameResolver() {
        return transit.stopNameResolver();
    }

    /**
     * The multi-criteria state can handle multiple access/egress paths to a single stop, but the
     * Standard and BestTime states do not. To get a deterministic behaviour we filter the
     * paths and return the paths with the shortest duration for none multi-criteria search. If two
     * paths have the same duration the first one is picked.
     * <p>
     * This method is static and package local to enable unit-testing.
     */
    static Collection<RaptorTransfer> accessOrEgressPaths(
            boolean getAccess,
            RaptorProfile profile,
            SearchParams searchParams
    ) {
        var paths = getAccess
                ? searchParams.accessPaths()
                : searchParams.egressPaths();

        if(profile.is(RaptorProfile.MULTI_CRITERIA)) {
            return paths;
        }

        // For none MC-search we only want the fastest transfer for each stop,
        // no duplicates are accepted
        Map<Integer, RaptorTransfer> bestTimePaths = new HashMap<>();
        for (RaptorTransfer it : paths) {
            RaptorTransfer existing = bestTimePaths.get(it.stop());
            if(existing == null || it.durationInSeconds() < existing.durationInSeconds()) {
                bestTimePaths.put(it.stop(), it);
            }
        }
        return List.copyOf(bestTimePaths.values());
    }
}
