package org.opentripplanner.transit.model.filter.transit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.api.request.TripRequest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.filter.expr.Matcher;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.timetable.Trip;

public class TripMatcherFactoryTest {

  private Trip tripRut;
  private Trip tripRut2;
  private Trip tripAkt;

  @BeforeEach
  void setup() {
    tripRut =
      Trip
        .of(new FeedScopedId("RUT:route:trip", "1"))
        .withRoute(
          Route
            .of(new FeedScopedId("RUT:route", "1"))
            .withAgency(
              Agency
                .of(new FeedScopedId("RUT", "1"))
                .withName("RUT")
                .withTimezone("Europe/Oslo")
                .build()
            )
            .withMode(TransitMode.BUS)
            .withShortName("BUS")
            .build()
        )
        .withServiceId(new FeedScopedId("RUT:route:trip", "1"))
        .build();
    tripRut2 =
      Trip
        .of(new FeedScopedId("RUT:route:trip", "2"))
        .withRoute(
          Route
            .of(new FeedScopedId("RUT:route", "2"))
            .withAgency(
              Agency
                .of(new FeedScopedId("RUT", "2"))
                .withName("RUT")
                .withTimezone("Europe/Oslo")
                .build()
            )
            .withMode(TransitMode.BUS)
            .withShortName("BUS")
            .build()
        )
        .withServiceId(new FeedScopedId("RUT:route:trip", "2"))
        .build();
    tripAkt =
      Trip
        .of(new FeedScopedId("AKT:route:trip", "1"))
        .withRoute(
          Route
            .of(new FeedScopedId("AKT:route", "1"))
            .withAgency(
              Agency
                .of(new FeedScopedId("AKT", "1"))
                .withName("AKT")
                .withTimezone("Europe/Oslo")
                .build()
            )
            .withMode(TransitMode.BUS)
            .withShortName("BUS")
            .build()
        )
        .withServiceId(new FeedScopedId("AKT:route:trip", "1"))
        .build();
  }

  @Test
  void testMatchRouteId() {
    TripRequest request = TripRequest
      .of()
      .withRoutes(List.of(new FeedScopedId("RUT:route", "1")))
      .build();

    Matcher<Trip> matcher = TripMatcherFactory.of(request, feedScopedId -> Set.of());

    assertTrue(matcher.match(tripRut));
    assertFalse(matcher.match(tripRut2));
    assertFalse(matcher.match(tripAkt));
  }

  @Test
  void testMatchDefaultAll() {
    TripRequest request = TripRequest.of().build();

    Matcher<Trip> matcher = TripMatcherFactory.of(request, feedScopedId -> Set.of());

    assertTrue(matcher.match(tripRut));
    assertTrue(matcher.match(tripRut2));
    assertTrue(matcher.match(tripAkt));
  }

  @Test
  void testMatchAgencyId() {
    TripRequest request = TripRequest
      .of()
      .withAgencies(List.of(new FeedScopedId("RUT", "1")))
      .build();

    Matcher<Trip> matcher = TripMatcherFactory.of(request, feedScopedId -> Set.of());

    assertTrue(matcher.match(tripRut));
    assertFalse(matcher.match(tripRut2));
    assertFalse(matcher.match(tripAkt));
  }

  @Test
  void testMatchServiceDates() {
    TripRequest request = TripRequest
      .of()
      .withServiceDates(List.of(LocalDate.of(2024, 2, 22), LocalDate.of(2024, 2, 23)))
      .build();

    Matcher<Trip> matcher = TripMatcherFactory.of(request, this::dummyServiceDateProvider);

    assertTrue(matcher.match(tripRut));
    assertTrue(matcher.match(tripRut2));
    assertFalse(matcher.match(tripAkt));
  }

  private Set<LocalDate> dummyServiceDateProvider(FeedScopedId feedScopedId) {
    if (feedScopedId.equals(new FeedScopedId("RUT:route:trip", "1"))) {
      return Set.of(LocalDate.of(2024, 2, 22), LocalDate.of(2024, 2, 23));
    } else if (feedScopedId.equals(new FeedScopedId("RUT:route:trip", "2"))) {
      return Set.of(LocalDate.of(2024, 2, 23));
    }
    return Set.of();
  }
}
