package org.opentripplanner.transit.raptor.speed_test;

import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.map.TIntIntMap;
import org.opentripplanner.routing.algorithm.raptor.transit.SlackProvider;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.transit.raptor.api.request.Optimization;
import org.opentripplanner.transit.raptor.api.request.RaptorProfile;
import org.opentripplanner.transit.raptor.api.request.RaptorRequest;
import org.opentripplanner.transit.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.transit.raptor.speed_test.options.SpeedTestCmdLineOpts;
import org.opentripplanner.transit.raptor.speed_test.options.SpeedTestConfig;
import org.opentripplanner.transit.raptor.speed_test.testcase.TestCase;
import org.opentripplanner.transit.raptor.speed_test.transit.AccessEgressLeg;
import org.opentripplanner.transit.raptor.speed_test.transit.EgressAccessRouter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;


public class SpeedTestRequest {

    /**
     * This is used to expand the search window for all test cases to test the effect of long windows.
     * <p/>
     * REMEMBER TO CHANGE IT BACK TO 0 BEFORE VCS COMMIT.
     */
    private static final int EXPAND_SEARCH_WINDOW_HOURS = 0;

    private final TestCase testCase;
    private final SpeedTestCmdLineOpts opts;
    private final SpeedTestConfig config;
    private final ZoneId inputZoneId;
    private final LocalDate date;

    SpeedTestRequest(TestCase testCase, SpeedTestCmdLineOpts opts, SpeedTestConfig config, ZoneId inputZoneId) {
        this.testCase = testCase;
        this.opts = opts;
        this.config = config;
        this.date = config.testDate;
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

    Set<TransitMode> getTransitModes() {
        return new HashSet<>(EnumSet.of(
            TransitMode.BUS, TransitMode.RAIL, TransitMode.SUBWAY, TransitMode.TRAM));
    }

    double getWalkSpeedMeterPrSecond() {
        // 1.4 m/s = ~ 5.0 km/t
        return config.walkSpeedMeterPrSecond;
    }

    public double getAccessEgressMaxWalkDistanceMeters() {
        return config.maxWalkDistanceMeters;
    }


    RaptorRequest<TripSchedule> createRangeRaptorRequest(
            SpeedTestProfile profile,
            int numOfExtraTransfers,
            boolean oneIterationOnly,
            EgressAccessRouter streetRouter
    ) {
        // Add half of the extra time to departure and half to the arrival
        int expandSearchSec = EXPAND_SEARCH_WINDOW_HOURS * 3600/2;


        RaptorRequestBuilder<TripSchedule> builder = new RaptorRequestBuilder<>();
        builder.searchParams()
                .boardSlackInSeconds(120)
                .timetableEnabled(true)
                .numberOfAdditionalTransfers(numOfExtraTransfers);

        if(testCase.departureTime != TestCase.NOT_SET) {
            builder.searchParams().earliestDepartureTime(testCase.departureTime - expandSearchSec);
        }

        if(testCase.arrivalTime != TestCase.NOT_SET) {
            builder.searchParams().latestArrivalTime(testCase.arrivalTime + expandSearchSec);
        }

        if(testCase.window != TestCase.NOT_SET) {
            builder.searchParams().searchWindowInSeconds(testCase.window + 2 * expandSearchSec);
        }

        if(oneIterationOnly) {
            builder.searchParams().searchOneIterationOnly();
        }

        builder.enableOptimization(Optimization.PARALLEL);

        builder.profile(profile.raptorProfile);
        for (Optimization it : profile.optimizations) {
            builder.enableOptimization(it);
        }
        if(profile.raptorProfile.isOneOf(RaptorProfile.NO_WAIT_STD, RaptorProfile.NO_WAIT_BEST_TIME)) {
            builder.searchParams().searchOneIterationOnly();
        }

        builder.slackProvider(new SlackProvider(
                config.request.transferSlack,
                config.request.boardSlack,
                config.request.boardSlackForMode,
                config.request.alightSlack,
                config.request.alightSlackForMode
        ));

        builder.searchDirection(profile.direction);

        addAccessEgressStopArrivals(streetRouter.getAccessTimesInSecondsByStopIndex(), builder.searchParams()::addAccessStop);
        addAccessEgressStopArrivals(streetRouter.getEgressTimesInSecondsByStopIndex(), builder.searchParams()::addEgressStop);

        addDebugOptions(builder, opts);

        RaptorRequest<TripSchedule> req = builder.build();

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

    private static void addDebugOptions(RaptorRequestBuilder<TripSchedule> builder, SpeedTestCmdLineOpts opts) {
        List<Integer> stops = opts.debugStops();
        List<Integer> path = opts.debugPath();

        boolean debugLoggerEnabled = opts.debugRequest() || opts.debug();

        debugLoggerEnabled = debugLoggerEnabled || opts.compareHeuristics();

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
