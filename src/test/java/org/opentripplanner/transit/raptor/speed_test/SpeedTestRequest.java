package org.opentripplanner.transit.raptor.speed_test;

import static org.opentripplanner.transit.raptor._data.transit.TestTransfer.walk;
import static org.opentripplanner.transit.raptor.api.request.RaptorProfile.MIN_TRAVEL_DURATION;
import static org.opentripplanner.transit.raptor.api.request.RaptorProfile.MIN_TRAVEL_DURATION_BEST_TIME;

import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.map.TIntIntMap;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.opentripplanner.model.modes.AllowedTransitMode;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.SlackProvider;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.transit.raptor.rangeraptor.SystemErrDebugLogger;
import org.opentripplanner.transit.raptor.api.request.Optimization;
import org.opentripplanner.transit.raptor.api.request.RaptorRequest;
import org.opentripplanner.transit.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.speed_test.options.SpeedTestCmdLineOpts;
import org.opentripplanner.transit.raptor.speed_test.options.SpeedTestConfig;
import org.opentripplanner.transit.raptor.speed_test.testcase.TestCase;
import org.opentripplanner.transit.raptor.speed_test.transit.EgressAccessRouter;


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

    SpeedTestRequest(
            TestCase testCase,
            SpeedTestCmdLineOpts opts,
            SpeedTestConfig config,
            ZoneId inputZoneId
    ) {
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
                        + testCase.departureTime * 1000L
        );
    }

    ZonedDateTime getDepartureDateWithZone() {
        return ZonedDateTime.of(date, LocalTime.MIDNIGHT, inputZoneId);
    }

    Set<AllowedTransitMode> getTransitModes() {
        return AllowedTransitMode.getAllTransitModesExceptAirplane();
    }

    double getWalkSpeedMeterPrSecond() {
        // 1.4 m/s = ~ 5.0 km/t
        return config.walkSpeedMeterPrSecond;
    }

    public double getAccessEgressMaxWalkDurationSeconds() {
        return config.maxWalkDurationSeconds;
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
        if(profile.raptorProfile.isOneOf(MIN_TRAVEL_DURATION, MIN_TRAVEL_DURATION_BEST_TIME)) {
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

        builder.searchParams().addAccessPaths(
            mapToAccessEgress(streetRouter.getAccessTimesInSecondsByStopIndex())
        );
        builder.searchParams().addEgressPaths(
            mapToAccessEgress(streetRouter.getEgressTimesInSecondsByStopIndex())
        );

        addDebugOptions(builder, opts);

        RaptorRequest<TripSchedule> req = builder.build();

        if (opts.debugRequest()) {
            System.err.println("-> Request: " + req);
        }

        return req;
    }

    private static List<RaptorTransfer> mapToAccessEgress(TIntIntMap timesToStopsInSeconds) {
        List<RaptorTransfer> paths = new ArrayList<>();
        TIntIntIterator it = timesToStopsInSeconds.iterator();
        while (it.hasNext()) {
            it.advance();
            paths.add(walk(it.key(), it.value()));
        }
        return paths;
    }

    private static void addDebugOptions(
            RaptorRequestBuilder<TripSchedule> builder,
            SpeedTestCmdLineOpts opts
    ) {
        List<Integer> stops = opts.debugStops();
        List<Integer> path = opts.debugPath();

        boolean debugLoggerEnabled = opts.debugRequest() || opts.debug();

        debugLoggerEnabled = debugLoggerEnabled || opts.compareHeuristics();

        if(!debugLoggerEnabled && stops.isEmpty() && path.isEmpty()) {
            return;
        }

        SystemErrDebugLogger logger = new SystemErrDebugLogger(debugLoggerEnabled);
        builder.debug()
                .stopArrivalListener(logger::stopArrivalLister)
                .pathFilteringListener(logger::pathFilteringListener)
                .logger(logger)
                .setPath(path)
                .debugPathFromStopIndex(opts.debugPathFromStopIndex())
                .addStops(stops);

    }
}
