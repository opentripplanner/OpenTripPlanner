package org.opentripplanner.transit.raptor.speed_test;

import java.time.Duration;
import java.util.List;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.stream.Collectors;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.transit.raptor.speed_test.options.SpeedTestCmdLineOpts;
import org.opentripplanner.transit.raptor.speed_test.options.SpeedTestConfig;
import org.opentripplanner.transit.raptor.speed_test.testcase.TestCase;

import static org.opentripplanner.transit.raptor.api.request.RaptorProfile.MIN_TRAVEL_DURATION;
import static org.opentripplanner.transit.raptor.api.request.RaptorProfile.MIN_TRAVEL_DURATION_BEST_TIME;


public class SpeedTestRequest {
    private final TestCase testCase;
    private final SpeedTestCmdLineOpts opts;
    private final SpeedTestConfig config;
    private final SpeedTestProfile profile;
    private final ZoneId timeZoneId;

    SpeedTestRequest(
            TestCase testCase,
            SpeedTestCmdLineOpts opts,
            SpeedTestConfig config,
            SpeedTestProfile profile,
            ZoneId timeZoneId
    ) {
        this.testCase = testCase;
        this.opts = opts;
        this.config = config;
        this.profile = profile;
        this.timeZoneId = timeZoneId;
    }

    public TestCase tc() {
        return testCase;
    }

    RoutingRequest toRoutingRequest() {
        var request = config.request.clone();

        if (testCase.departureTime != TestCase.NOT_SET) {
            request.setDateTime(time(testCase.departureTime));
            request.arriveBy = false;
            if (testCase.arrivalTime != TestCase.NOT_SET) {
                request.raptorOptions.withTimeLimit(time(testCase.arrivalTime));
            }
        } else if (testCase.arrivalTime != TestCase.NOT_SET) {
            request.setDateTime(time(testCase.arrivalTime));
            request.arriveBy = true;
        }

        if (testCase.window != TestCase.NOT_SET) {
            request.searchWindow = Duration.ofSeconds(testCase.window);
        }

        request.from = testCase.fromPlace.toGenericLocation();
        request.to = testCase.toPlace.toGenericLocation();
        request.numItineraries = opts.numOfItineraries();
        request.modes = testCase.getModes();

        request.raptorOptions
                .withProfile(profile.raptorProfile)
                .withOptimizations(profile.optimizations)
                .withSearchDirection(profile.direction);


        if (profile.raptorProfile.isOneOf(MIN_TRAVEL_DURATION, MIN_TRAVEL_DURATION_BEST_TIME)) {
            request.searchWindow = Duration.ZERO;
        }

        addDebugOptions(request, opts);

        return request;
    }

    List<String> tags() {
        return testCase.tags.stream().distinct().sorted().collect(Collectors.toList());
    }

    private static void addDebugOptions(
            RoutingRequest request,
            SpeedTestCmdLineOpts opts
    ) {
        request.raptorDebuging
                .withStops(opts.debugStops())
                .withPath(opts.debugPath());
    }

    private Instant time(int time) {
        return LocalTime.ofSecondOfDay(time).atDate(config.testDate).atZone(timeZoneId).toInstant();
    }
}