package org.opentripplanner.transit.raptor.speed_test;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.transit.raptor.rangeraptor.SystemErrDebugLogger;
import org.opentripplanner.transit.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.transit.raptor.speed_test.options.SpeedTestCmdLineOpts;
import org.opentripplanner.transit.raptor.speed_test.options.SpeedTestConfig;
import org.opentripplanner.transit.raptor.speed_test.testcase.TestCase;


public class SpeedTestRequest {

    /**
     * This is used to expand the search window for all test cases to test the effect of long windows.
     * <p/>
     * REMEMBER TO CHANGE IT BACK TO 0 BEFORE VCS COMMIT.
     */
    private static final int EXPAND_SEARCH_WINDOW_HOURS = 0;

    private final TestCase testCase;
    private final SpeedTestConfig config;
    private final ZonedDateTime departureTime;
    private final SpeedTestCmdLineOpts opts;

    SpeedTestRequest(
            TestCase testCase,
            SpeedTestCmdLineOpts opts,
            SpeedTestConfig config,
            ZonedDateTime departureTime
    ) {
        this.testCase = testCase;
        this.config = config;
        this.opts = opts;
        this.departureTime = departureTime;
    }

    public TestCase tc() { return testCase; }

    ZonedDateTime getDepartureTime() {
        return departureTime;
    }

    double walkSpeed() {
        // 1.4 m/s = ~ 5.0 km/t
        return config.walkSpeedMeterPrSecond;
    }

    RoutingRequest toRoutingRequest() {
        var routingRequest = new RoutingRequest();
        routingRequest.setDateTime(this.getDepartureTime().toInstant());
        routingRequest.from = this.tc().fromPlace.toGenericLocation();
        routingRequest.to = this.tc().toPlace.toGenericLocation();
        routingRequest.walkSpeed = this.walkSpeed();
        routingRequest.numItineraries = opts.numOfItineraries();
        routingRequest.searchWindow = Duration.ofSeconds(this.tc().window);
        routingRequest.modes = tc().getModes();

        return routingRequest;
    }

    List<String> tags() {
        return testCase.tags.stream().distinct().sorted().collect(Collectors.toList());
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
