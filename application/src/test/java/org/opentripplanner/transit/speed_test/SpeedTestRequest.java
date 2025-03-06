package org.opentripplanner.transit.speed_test;

import static org.opentripplanner.raptor.api.request.RaptorProfile.MIN_TRAVEL_DURATION;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.RoutingTag;
import org.opentripplanner.routing.api.request.request.filter.SelectRequest;
import org.opentripplanner.routing.api.request.request.filter.TransitFilterRequest;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
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

    if (input.window() != null) {
      request.setSearchWindow(input.window());
    }

    request.setFrom(input.fromPlace());
    request.setTo(input.toPlace());

    // Filter the results inside the SpeedTest, not in the itineraries filter,
    // when ignoring street results. This will use the default which is 50.
    if (!config.ignoreStreetResults) {
      request.setNumItineraries(opts.numOfItineraries());
    }
    request.journey().setModes(input.modes().getRequestModes());

    var tModes = input.modes().getTransitModes().stream().map(MainAndSubMode::new).toList();
    if (tModes.isEmpty()) {
      request.journey().transit().disable();
    } else {
      var builder = TransitFilterRequest.of()
        .addSelect(SelectRequest.of().withTransportModes(tModes).build());
      request.journey().transit().setFilters(List.of(builder.build()));
    }

    if (profile.raptorProfile().is(MIN_TRAVEL_DURATION)) {
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
