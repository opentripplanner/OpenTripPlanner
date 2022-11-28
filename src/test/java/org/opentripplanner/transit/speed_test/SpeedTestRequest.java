package org.opentripplanner.transit.speed_test;

import static org.opentripplanner.raptor.api.request.RaptorProfile.MIN_TRAVEL_DURATION;
import static org.opentripplanner.raptor.api.request.RaptorProfile.MIN_TRAVEL_DURATION_BEST_TIME;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.RoutingTag;
import org.opentripplanner.transit.speed_test.model.SpeedTestProfile;
import org.opentripplanner.transit.speed_test.model.testcase.TestCase;
import org.opentripplanner.transit.speed_test.options.SpeedTestCmdLineOpts;
import org.opentripplanner.transit.speed_test.options.SpeedTestConfig;

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

  RouteRequest toRouteRequest() {
    var request = config.request.clone();

    var input = testCase.definition();

    if (input.departureTimeSet()) {
      request.setDateTime(time(input.departureTime()));
      request.setArriveBy(false);
    } else if (input.arrivalTimeSet()) {
      request.setDateTime(time(input.arrivalTime()));
      request.setArriveBy(true);
    }

    if (input.window() != TestCase.NOT_SET) {
      request.setSearchWindow(Duration.ofSeconds(input.window()));
    }

    request.setFrom(input.fromPlace());
    request.setTo(input.toPlace());
    request.setNumItineraries(opts.numOfItineraries());
    request.journey().setModes(input.modes());

    if (profile.raptorProfile().isOneOf(MIN_TRAVEL_DURATION, MIN_TRAVEL_DURATION_BEST_TIME)) {
      request.setSearchWindow(Duration.ZERO);
    }

    request
      .journey()
      .transit()
      .raptorDebugging()
      .withStops(opts.debugStops())
      .withPath(opts.debugPath());

    request.withPreferences(pref -> {
      if (input.departureTimeSet() && input.arrivalTimeSet()) {
        pref.withTransit(transit ->
          transit.withRaptor(r -> r.withTimeLimit(time(input.arrivalTime())))
        );
      }
      pref.withTransit(transit ->
        transit.withRaptor(raptor ->
          raptor
            .withProfile(profile.raptorProfile())
            .withOptimizations(profile.optimizations())
            .withSearchDirection(profile.direction())
        )
      );
      pref.withSystem(it ->
        it.addTags(
          List.of(
            RoutingTag.testCaseSample(input.idAndDescription()),
            RoutingTag.testCaseCategory(input.category())
          )
        )
      );
    });

    return request;
  }

  private Instant time(int time) {
    // Note time may be negative and exceed 24 hours
    return config.testDate.atStartOfDay(timeZoneId).plusSeconds(time).toInstant();
  }
}
