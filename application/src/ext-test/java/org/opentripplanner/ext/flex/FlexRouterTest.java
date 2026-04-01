package org.opentripplanner.ext.flex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transfer.regular.TransferServiceTestFactory.defaultTransferService;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.flex.template.FlexServiceDate;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.service.streetdetails.internal.DefaultStreetDetailsRepository;
import org.opentripplanner.service.streetdetails.internal.DefaultStreetDetailsService;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transit.api.request.TripRequest;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TripInput;

class FlexRouterTest {

  private final String ROUTE1 = "route1";
  private final String ROUTE2 = "route2";
  private final String TRIP1 = "trip1";
  private final String TRIP2 = "trip2";
  private final String TRIP3 = "trip3";

  private final LocalDate day0 = LocalDate.of(2025, 2, 28);
  private final LocalDate day1 = day0.plusDays(1);
  private final LocalDate day2 = day1.plusDays(1);
  private final LocalDate day3 = day2.plusDays(1);

  @BeforeAll
  static void setup() {
    OTPFeature.enableFeatures(Map.of(OTPFeature.FlexRouting, true));
  }

  @Test
  void runningDateToServiceDateAggregation() {
    var subject = setupEnvironment(TripRequest.of().build());
    var dates = subject.flexServiceDates();

    assertEquals(4, dates.size());
    assertTripsForDate(day0, dates, TRIP3);
    assertTripsForDate(day1, dates, TRIP1, TRIP2);
    assertTripsForDate(day2, dates, TRIP1, TRIP2, TRIP3);
    assertTripsForDate(day3, dates, TRIP2, TRIP3);
  }

  @Test
  void runningDateToServiceDateAggregationWithFiltering() {
    var subject = setupEnvironment(
      TripRequest.of().withIncludeRoutes(List.of(new FeedScopedId("F", ROUTE1))).build()
    );

    var dates = subject.flexServiceDates();
    assertEquals(2, dates.size());
    assertTripsForDate(day1, dates, TRIP1);
    assertTripsForDate(day2, dates, TRIP1);
  }

  private void assertTripsForDate(
    LocalDate serviceDate,
    Collection<FlexServiceDate> flexServiceDates,
    String... expectedTripIds
  ) {
    var flexServiceDate = flexServiceDates
      .stream()
      .filter(fsd -> fsd.serviceDate().equals(serviceDate))
      .findFirst()
      .orElseThrow();

    var actualTripIds = flexServiceDate
      .tripsRunning()
      .stream()
      .map(FlexTrip::getId)
      .map(FeedScopedId::getId)
      .collect(Collectors.toSet());

    assertEquals(Set.of(expectedTripIds), actualTripIds);
  }

  private @NonNull FlexRouter setupEnvironment(TripRequest tripRequest) {
    // Test FlexTrip running day -> service date aggregation using three trips:
    //     trip1 runs on it's service day
    //     trip2 runs on it's service day and the day after
    //     trip3 runs on the day after it's service day

    var envBuilder = TransitTestEnvironment.of();
    var stop = envBuilder.areaStop("stop");
    var route1 = envBuilder.route(ROUTE1);
    var route2 = envBuilder.route(ROUTE2);

    envBuilder
      .timetable()
      .trip(
        TripInput.flex("trip1")
          .withRoute(route1)
          .addStop(stop, "08:00:00", "16:00:00")
          .addStop(stop, "08:00:00", "16:00:00")
          .withServiceDates(day1, day2)
      );

    envBuilder
      .timetable()
      .trip(
        TripInput.flex("trip2")
          .withRoute(route2)
          .addStop(stop, "08:00:00", "26:00:00")
          .addStop(stop, "08:00:00", "26:00:00")
          .withServiceDates(day1, day2, day3)
      );

    envBuilder
      .timetable()
      .trip(
        TripInput.flex("trip3")
          .withRoute(route2)
          .addStop(stop, "24:00:00", "26:00:00")
          .addStop(stop, "24:00:00", "26:00:00")
          .withServiceDates(day0, day2, day3)
      );

    var env = envBuilder.build();
    var requestedTime = day1.atStartOfDay(env.timeZone()).toInstant();

    return new FlexRouter(
      new Graph(),
      env.transitService(),
      defaultTransferService(),
      new DefaultStreetDetailsService(new DefaultStreetDetailsRepository()),
      FlexParameters.defaultValues(),
      tripRequest,
      requestedTime,
      null,
      0,
      2,
      List.of(),
      List.of()
    );
  }

  @AfterAll
  static void teardown() {
    OTPFeature.enableFeatures(Map.of(OTPFeature.FlexRouting, false));
  }
}
