package org.opentripplanner.transit.raptor.speed_test;

import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.map.TIntIntMap;
import org.opentripplanner.transit.raptor.api.request.Optimization;
import org.opentripplanner.transit.raptor.api.request.RangeRaptorProfile;
import org.opentripplanner.transit.raptor.api.request.RangeRaptorRequest;
import org.opentripplanner.transit.raptor.api.request.RequestBuilder;
import org.opentripplanner.transit.raptor.speed_test.cli.CommandLineOpts;
import org.opentripplanner.transit.raptor.speed_test.cli.SpeedTestCmdLineOpts;
import org.opentripplanner.transit.raptor.speed_test.test.TestCase;
import org.opentripplanner.transit.raptor.speed_test.transit.AccessEgressLeg;
import org.opentripplanner.transit.raptor.speed_test.transit.EgressAccessRouter;
import org.opentripplanner.transit.raptor.transitadapter.TripScheduleAdapter;

import java.util.List;
import java.util.function.Consumer;


/**
 * Help SpeedTast with mapping to new {@link RangeRaptorRequest}.
 */
class RequestSupport {

    /**
     * This is used to expand the search window for all test cases to test the effect of long windows.
     * <p/>
     * REMEMBER TO CHANGE IT BACK TO 0 BEFORE VCS COMMIT.
     */
    private static final int EXPAND_SEARCH_WINDOW_HOURS = 0;

    /** Private to prevent it from instantiation. This is a utility class with ONLY static methods. */
    private RequestSupport() { }


    static RangeRaptorRequest<TripScheduleAdapter> xcreateRangeRaptorRequest(
            TestCase testCase,
            CommandLineOpts opts,
            SpeedTestProfile profile,
            int latestArrivalTime,
            int numOfExtraTransfers,
            boolean oneIterationOnly,
            EgressAccessRouter streetRouter
    ) {
        // Add half of the extra time to departure and half to the arrival
        int expandDeltaSeconds = EXPAND_SEARCH_WINDOW_HOURS * 3600/2;


        RequestBuilder<TripScheduleAdapter> builder = new RequestBuilder<>();
        builder.searchParams()
                .boardSlackInSeconds(120)
                .timetableEnabled(false)
                .earliestDepartureTime(testCase.departureTime - expandDeltaSeconds)
                .latestArrivalTime(latestArrivalTime + expandDeltaSeconds)
                .searchWindowInSeconds(testCase.window + 2 * expandDeltaSeconds)
                .numberOfAdditionalTransfers(numOfExtraTransfers);

        if(oneIterationOnly) {
            builder.searchParams().searchOneIterationOnly();
        }

        builder.enableOptimization(Optimization.PARALLEL);

        builder.profile(profile.raptorProfile);
        for (Optimization it : profile.optimizations) {
            builder.enableOptimization(it);
        }
        if(profile.raptorProfile.isOneOf(RangeRaptorProfile.NO_WAIT_STD, RangeRaptorProfile.NO_WAIT_BEST_TIME)) {
            builder.searchParams().searchOneIterationOnly();
        }

        builder.searchDirection(profile.forward);

        addAccessEgressStopArrivals(streetRouter.accessTimesToStopsInSeconds, builder.searchParams()::addAccessStop);
        addAccessEgressStopArrivals(streetRouter.egressTimesToStopsInSeconds, builder.searchParams()::addEgressStop);

        addDebugOptions(builder, opts);

        RangeRaptorRequest<TripScheduleAdapter> req = builder.build();

        if (opts.debugRequest()) {
            System.err.println("-> Request: " + req);
        }

        return req;
    }


    private static void addAccessEgressStopArrivals(TIntIntMap timesToStopsInSeconds, Consumer<AccessEgressLeg> addStop) {
        for(TIntIntIterator it = timesToStopsInSeconds.iterator(); it.hasNext(); ) {
            it.advance();
            addStop.accept(new AccessEgressLeg(it.key(), it.value()));
        }
    }

    private static void addDebugOptions(RequestBuilder<TripScheduleAdapter> builder, CommandLineOpts opts) {
        List<Integer> stops = opts.debugStops();
        List<Integer> path = opts.debugPath();

        boolean debugLoggerEnabled = opts.debugRequest() || opts.debug();

        if(opts instanceof SpeedTestCmdLineOpts) {
            debugLoggerEnabled = debugLoggerEnabled || ((SpeedTestCmdLineOpts)opts).compareHeuristics();
        }

        if(!debugLoggerEnabled && stops.isEmpty() && path.isEmpty()) {
            return;
        }

        DebugLogger<TripScheduleAdapter> logger = new DebugLogger<>(debugLoggerEnabled);

        builder.debug()
                .stopArrivalListener(logger::stopArrivalLister)
                .pathFilteringListener(logger::pathFilteringListener)
                .logger(logger)
                .debugPathFromStopIndex(opts.debugPathFromStopIndex());
        builder.debug().path().addAll(path);
        builder.debug().stops().addAll(stops);
    }
}
