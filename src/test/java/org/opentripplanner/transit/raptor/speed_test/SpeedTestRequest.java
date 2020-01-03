package org.opentripplanner.transit.raptor.speed_test;

import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.map.TIntIntMap;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.transit.raptor.api.request.Optimization;
import org.opentripplanner.transit.raptor.api.request.RangeRaptorProfile;
import org.opentripplanner.transit.raptor.api.request.RangeRaptorRequest;
import org.opentripplanner.transit.raptor.api.request.RequestBuilder;
import org.opentripplanner.transit.raptor.api.request.TuningParameters;
import org.opentripplanner.transit.raptor.speed_test.cli.CommandLineOpts;
import org.opentripplanner.transit.raptor.speed_test.cli.SpeedTestCmdLineOpts;
import org.opentripplanner.transit.raptor.speed_test.testcase.TestCase;
import org.opentripplanner.transit.raptor.speed_test.transit.AccessEgressLeg;
import org.opentripplanner.transit.raptor.speed_test.transit.EgressAccessRouter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;


public class SpeedTestRequest {

    /**
     * This is used to expand the search window for all test cases to test the effect of long windows.
     * <p/>
     * REMEMBER TO CHANGE IT BACK TO 0 BEFORE VCS COMMIT.
     */
    private static final int EXPAND_SEARCH_WINDOW_HOURS = 0;

    static final TuningParameters TUNING_PARAMETERS = new TuningParameters() {
        @Override
        public int maxNumberOfTransfers() {
            return 12;
        }

        @Override
        public int searchThreadPoolSize() {
            return 0;
        }
    };

    private final ZoneId inputZoneId;
    private final TestCase testCase;
    private final SpeedTestCmdLineOpts opts;
    private final LocalDate date = LocalDate.of(2019, 11, 14);

    SpeedTestRequest(TestCase testCase, SpeedTestCmdLineOpts opts, ZoneId inputZoneId) {
        this.testCase = testCase;
        this.opts = opts;
        this.inputZoneId = inputZoneId;
    }

    public TestCase tc() { return testCase; }
    public LocalDate getDepartureDate() {
        return date;
    }
    public Date getDepartureTimestamp() {
        return new Date(
                date.atStartOfDay(inputZoneId).toInstant().toEpochMilli()
                        + testCase.departureTime * 1000
        );
    }
    ZonedDateTime getDepartureDateWithZone() {
        return ZonedDateTime.of(date, LocalTime.MIDNIGHT, inputZoneId);
    }
    public int getArrivalTime() {
        return testCase.arrivalTime;
    }
    TraverseModeSet getTransitModes() {
        return new TraverseModeSet(TraverseMode.BUS, TraverseMode.RAIL, TraverseMode.SUBWAY, TraverseMode.TRAM);
    }
    public EnumSet<TraverseMode> getAccessEgressModes() {
        return EnumSet.of(TraverseMode.WALK);
    }
    double getWalkSpeedMeterPrSecond() {
        // 1.4 m/s = ~ 5.0 km/t
        return 1.4;
    }
    public double getAccessEgressMaxWalkDistanceMeters() {
        return 300;
    }


    RangeRaptorRequest<TripSchedule> createRangeRaptorRequest(
            SpeedTestProfile profile,
            int latestArrivalTime,
            int numOfExtraTransfers,
            boolean oneIterationOnly,
            EgressAccessRouter streetRouter
    ) {
        // Add half of the extra time to departure and half to the arrival
        int expandDeltaSeconds = EXPAND_SEARCH_WINDOW_HOURS * 3600/2;


        RequestBuilder<TripSchedule> builder = new RequestBuilder<>();
        builder.searchParams()
                .boardSlackInSeconds(120)
                .timetableEnabled(true)
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

        addAccessEgressStopArrivals(streetRouter.accessTimesInSecondsByStopIndex, builder.searchParams()::addAccessStop);
        addAccessEgressStopArrivals(streetRouter.egressTimesInSecondsByStopIndex, builder.searchParams()::addEgressStop);

        addDebugOptions(builder, opts);

        RangeRaptorRequest<TripSchedule> req = builder.build();

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

    private static void addDebugOptions(RequestBuilder<TripSchedule> builder, CommandLineOpts opts) {
        List<Integer> stops = opts.debugStops();
        List<Integer> path = opts.debugPath();

        boolean debugLoggerEnabled = opts.debugRequest() || opts.debug();

        if(opts instanceof SpeedTestCmdLineOpts) {
            debugLoggerEnabled = debugLoggerEnabled || ((SpeedTestCmdLineOpts)opts).compareHeuristics();
        }

        if(!debugLoggerEnabled && stops.isEmpty() && path.isEmpty()) {
            return;
        }

        SpeedTestDebugLogger<TripSchedule> logger = new SpeedTestDebugLogger<>(debugLoggerEnabled);

        builder.debug()
                .stopArrivalListener(logger::stopArrivalLister)
                .pathFilteringListener(logger::pathFilteringListener)
                .logger(logger)
                .debugPathFromStopIndex(opts.debugPathFromStopIndex());
        builder.debug().path().addAll(path);
        builder.debug().stops().addAll(stops);
    }
}
