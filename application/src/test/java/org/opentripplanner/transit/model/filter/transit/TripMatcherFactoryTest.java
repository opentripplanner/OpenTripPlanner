package org.opentripplanner.transit.model.filter.transit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    tripRut = Trip.of(new FeedScopedId("F", "RUT:route:trip:1"))
      .withRoute(
        Route.of(new FeedScopedId("F", "RUT:route:1"))
          .withAgency(
            Agency.of(new FeedScopedId("F", "RUT:1"))
              .withName("RUT")
              .withTimezone("Europe/Oslo")
              .build()
          )
          .withMode(TransitMode.BUS)
          .withShortName("BUS")
          .build()
      )
      .withServiceId(new FeedScopedId("F", "RUT:route:trip:1"))
      .build();
    tripRut2 = Trip.of(new FeedScopedId("F", "RUT:route:trip:2"))
      .withRoute(
        Route.of(new FeedScopedId("F", "RUT:route:2"))
          .withAgency(
            Agency.of(new FeedScopedId("F", "RUT:2"))
              .withName("RUT")
              .withTimezone("Europe/Oslo")
              .build()
          )
          .withMode(TransitMode.BUS)
          .withShortName("BUS")
          .build()
      )
      .withServiceId(new FeedScopedId("F", "RUT:route:trip:2"))
      .build();
    tripAkt = Trip.of(new FeedScopedId("F", "AKT:route:trip:1"))
      .withRoute(
        Route.of(new FeedScopedId("F", "AKT:route:1"))
          .withAgency(
            Agency.of(new FeedScopedId("F", "AKT"))
              .withName("AKT")
              .withTimezone("Europe/Oslo")
              .build()
          )
          .withMode(TransitMode.BUS)
          .withShortName("BUS")
          .build()
      )
      .withServiceId(new FeedScopedId("F", "AKT:route:trip:1"))
      .build();
  }

  @Test
  void testMatchRouteId() {
    TripRequest request = TripRequest.of()
      .withIncludeRoutes(List.of(new FeedScopedId("F", "RUT:route:1")))
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
  void testNullListMatchesAll() {
    TripRequest request = TripRequest.of().withIncludeAgencies(null).build();

    Matcher<Trip> matcher = TripMatcherFactory.of(request, feedScopedId -> Set.of());

    assertTrue(matcher.match(tripRut));
    assertTrue(matcher.match(tripRut2));
    assertTrue(matcher.match(tripAkt));
  }

  @Test
  void testEmptyListMatchesNone() {
    TripRequest request = TripRequest.of().withIncludeAgencies(List.of()).build();

    Matcher<Trip> matcher = TripMatcherFactory.of(request, feedScopedId -> Set.of());

    assertFalse(matcher.match(tripRut));
    assertFalse(matcher.match(tripRut2));
    assertFalse(matcher.match(tripAkt));
  }

  @Test
  void testMatchAgencyId() {
    TripRequest request = TripRequest.of()
      .withIncludeAgencies(List.of(new FeedScopedId("F", "RUT:1")))
      .build();

    Matcher<Trip> matcher = TripMatcherFactory.of(request, feedScopedId -> Set.of());

    assertTrue(matcher.match(tripRut));
    assertFalse(matcher.match(tripRut2));
    assertFalse(matcher.match(tripAkt));
  }

  @Test
  void testMatchServiceDates() {
    TripRequest request = TripRequest.of()
      .withIncludeServiceDates(List.of(LocalDate.of(2024, 2, 22), LocalDate.of(2024, 2, 23)))
      .build();

    Matcher<Trip> matcher = TripMatcherFactory.of(request, this::dummyServiceDateProvider);

    assertTrue(matcher.match(tripRut));
    assertTrue(matcher.match(tripRut2));
    assertFalse(matcher.match(tripAkt));
  }

  private Set<LocalDate> dummyServiceDateProvider(FeedScopedId feedScopedId) {
    if (feedScopedId.equals(new FeedScopedId("F", "RUT:route:trip:1"))) {
      return Set.of(LocalDate.of(2024, 2, 22), LocalDate.of(2024, 2, 23));
    } else if (feedScopedId.equals(new FeedScopedId("F", "RUT:route:trip:2"))) {
      return Set.of(LocalDate.of(2024, 2, 23));
    }
    return Set.of();
  }
}
