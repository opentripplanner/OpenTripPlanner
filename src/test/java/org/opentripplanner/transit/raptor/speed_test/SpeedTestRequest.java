package org.opentripplanner.transit.raptor.speed_test;

import static org.opentripplanner.transit.raptor.api.request.RaptorProfile.MIN_TRAVEL_DURATION;
import static org.opentripplanner.transit.raptor.api.request.RaptorProfile.MIN_TRAVEL_DURATION_BEST_TIME;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.RoutingTag;
import org.opentripplanner.transit.raptor.speed_test.model.SpeedTestProfile;
import org.opentripplanner.transit.raptor.speed_test.model.testcase.TestCase;
import org.opentripplanner.transit.raptor.speed_test.options.SpeedTestCmdLineOpts;
import org.opentripplanner.transit.raptor.speed_test.options.SpeedTestConfig;

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

  RouteRequest toRoutingRequest() {
    var request = config.request.clone();
    var pref = request.preferences();

    var input = testCase.definition();

    if (input.departureTime() != TestCase.NOT_SET) {
      request.setDateTime(time(input.departureTime()));
      request.setArriveBy(false);
      if (input.arrivalTime() != TestCase.NOT_SET) {
        pref.transit().raptorOptions().withTimeLimit(time(input.arrivalTime()));
      }
    } else if (input.arrivalTime() != TestCase.NOT_SET) {
      request.setDateTime(time(input.arrivalTime()));
      request.setArriveBy(true);
    }

    if (input.window() != TestCase.NOT_SET) {
      request.setSearchWindow(Duration.ofSeconds(input.window()));
    }

    request.setFrom(input.fromPlace());
    request.setTo(input.toPlace());
    request.setNumItineraries(opts.numOfItineraries());
    request.modes = input.modes();

    pref
      .transit()
      .raptorOptions()
      .withProfile(profile.raptorProfile())
      .withOptimizations(profile.optimizations())
      .withSearchDirection(profile.direction());

    if (profile.raptorProfile().isOneOf(MIN_TRAVEL_DURATION, MIN_TRAVEL_DURATION_BEST_TIME)) {
      request.setSearchWindow(Duration.ZERO);
    }

    request
      .journey()
      .transit()
      .raptorDebugging()
      .withStops(opts.debugStops())
      .withPath(opts.debugPath());

    pref
      .system()
      .tags()
      .addAll(
        List.of(
          RoutingTag.testCaseSample(input.idAndDescription()),
          RoutingTag.testCaseCategory(input.category())
        )
      );
    return request;
  }

  private Instant time(int time) {
    // Note time may be negative and exceed 24 hours
    return config.testDate.atStartOfDay(timeZoneId).plusSeconds(time).toInstant();
  }
}
