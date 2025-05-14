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
    var builder = config.request.copyOf();

    var input = testCase.definition();

    if (input.departureTimeSet()) {
      builder.withDateTime(time(input.departureTime())).withArriveBy(false);
    } else if (input.arrivalTimeSet()) {
      builder.withDateTime(time(input.arrivalTime())).withArriveBy(true);
    }

    if (input.window() != null) {
      builder.withSearchWindow(input.window());
    }

    builder.withFrom(input.fromPlace()).withTo(input.toPlace());

    // Filter the results inside the SpeedTest, not in the itineraries filter,
    // when ignoring street results. This will use the default which is 50.
    if (!config.ignoreStreetResults) {
      builder.withNumItineraries(opts.numOfItineraries());
    }
    builder.withJourney(journeyBuilder -> {
      journeyBuilder.setModes(input.modes().getRequestModes());

      var tModes = input.modes().getTransitModes().stream().map(MainAndSubMode::new).toList();
      if (tModes.isEmpty()) {
        journeyBuilder.withTransit(b -> b.disable());
      } else {
        var fb = TransitFilterRequest.of()
          .addSelect(SelectRequest.of().withTransportModes(tModes).build());
        journeyBuilder.withTransit(b -> b.setFilters(List.of(fb.build())));
      }

      if (profile.raptorProfile().is(MIN_TRAVEL_DURATION)) {
        builder.withSearchWindow(Duration.ZERO);
      }

      journeyBuilder.withTransit(transitBuilder ->
        transitBuilder.withRaptorDebugging(d ->
          d.withStops(opts.debugStops()).withPath(opts.debugPath())
        )
      );
    });

    builder.withPreferences(pref -> {
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

    return builder.buildRequest();
  }

  private Instant time(int time) {
    // Note time may be negative and exceed 24 hours
    return config.testDate.atStartOfDay(timeZoneId).plusSeconds(time).toInstant();
  }
}
