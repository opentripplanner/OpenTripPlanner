package org.opentripplanner.transit.model.filter.transit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

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

  private static final FeedScopedId AKT_ID = id("AKT");
  private static final FeedScopedId RUTER1_ID = id("RUT:1");
  private static final FeedScopedId RUTER2_ID = id("RUT:2");
  private static final FeedScopedId RUTER_ROUTE1_ID = id("RUT:route:1");
  private Trip tripRut;
  private Trip tripRut2;
  private Trip tripAkt;

  @BeforeEach
  void setup() {
    tripRut = Trip.of(id("RUT:route:trip:1"))
      .withRoute(
        Route.of(RUTER_ROUTE1_ID)
          .withAgency(Agency.of(RUTER1_ID).withName("RUT").withTimezone("Europe/Oslo").build())
          .withMode(TransitMode.BUS)
          .withShortName("BUS")
          .build()
      )
      .withServiceId(id("RUT:route:trip:1"))
      .build();
    tripRut2 = Trip.of(id("RUT:route:trip:2"))
      .withRoute(
        Route.of(id("RUT:route:2"))
          .withAgency(Agency.of(RUTER2_ID).withName("RUT").withTimezone("Europe/Oslo").build())
          .withMode(TransitMode.BUS)
          .withShortName("BUS")
          .build()
      )
      .withServiceId(id("RUT:route:trip:2"))
      .build();
    tripAkt = Trip.of(id("AKT:route:trip:1"))
      .withRoute(
        Route.of(id("AKT:route:1"))
          .withAgency(Agency.of(AKT_ID).withName("AKT").withTimezone("Europe/Oslo").build())
          .withMode(TransitMode.BUS)
          .withShortName("BUS")
          .build()
      )
      .withServiceId(id("AKT:route:trip:1"))
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
  void testMatchExcludeRouteId() {
    TripRequest request = TripRequest.of().withExcludeRoutes(List.of(RUTER_ROUTE1_ID)).build();

    Matcher<Trip> matcher = TripMatcherFactory.of(request, feedScopedId -> Set.of());

    assertFalse(matcher.match(tripRut));
    assertTrue(matcher.match(tripRut2));
    assertTrue(matcher.match(tripAkt));
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
  void testMatchIncludeAgencyId() {
    TripRequest request = TripRequest.of().withIncludeAgencies(List.of(RUTER1_ID)).build();

    Matcher<Trip> matcher = TripMatcherFactory.of(request, feedScopedId -> Set.of());

    assertTrue(matcher.match(tripRut));
    assertFalse(matcher.match(tripRut2));
    assertFalse(matcher.match(tripAkt));
  }

  @Test
  void testMatchExcludeAgencyId() {
    TripRequest request = TripRequest.of().withExcludeAgencies(List.of(RUTER1_ID)).build();

    Matcher<Trip> matcher = TripMatcherFactory.of(request, feedScopedId -> Set.of());

    assertFalse(matcher.match(tripRut));
    assertTrue(matcher.match(tripRut2));
    assertTrue(matcher.match(tripAkt));
  }

  @Test
  void testIncludeNoAgencies() {
    TripRequest request = TripRequest.of().withIncludeAgencies(List.of()).build();

    Matcher<Trip> matcher = TripMatcherFactory.of(request, feedScopedId -> Set.of());

    assertFalse(matcher.match(tripRut));
    assertFalse(matcher.match(tripRut2));
    assertFalse(matcher.match(tripAkt));
  }

  @Test
  void testIncludeNoRoutes() {
    TripRequest request = TripRequest.of().withIncludeRoutes(List.of()).build();

    Matcher<Trip> matcher = TripMatcherFactory.of(request, feedScopedId -> Set.of());

    assertFalse(matcher.match(tripRut));
    assertFalse(matcher.match(tripRut2));
    assertFalse(matcher.match(tripAkt));
  }

  @Test
  void testDisjointAgencyFilters() {
    TripRequest request = TripRequest.of()
      .withIncludeAgencies(List.of(RUTER1_ID))
      .withExcludeAgencies(List.of(AKT_ID))
      .build();

    Matcher<Trip> matcher = TripMatcherFactory.of(request, feedScopedId -> Set.of());

    assertTrue(matcher.match(tripRut));
    assertFalse(matcher.match(tripRut2));
    assertFalse(matcher.match(tripAkt));
  }

  @Test
  void testIntersectingAgencyFilters() {
    TripRequest request = TripRequest.of()
      .withIncludeAgencies(List.of(RUTER1_ID, RUTER2_ID, AKT_ID))
      .withExcludeAgencies(List.of(AKT_ID))
      .build();

    Matcher<Trip> matcher = TripMatcherFactory.of(request, feedScopedId -> Set.of());

    assertTrue(matcher.match(tripRut));
    assertTrue(matcher.match(tripRut2));
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
    if (feedScopedId.equals(id("RUT:route:trip:1"))) {
      return Set.of(LocalDate.of(2024, 2, 22), LocalDate.of(2024, 2, 23));
    } else if (feedScopedId.equals(id("RUT:route:trip:2"))) {
      return Set.of(LocalDate.of(2024, 2, 23));
    }
    return Set.of();
  }
}
