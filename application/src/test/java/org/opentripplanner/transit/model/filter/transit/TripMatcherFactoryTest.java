package org.opentripplanner.transit.model.filter.transit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.transit.api.request.TripRequest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.filter.expr.Matcher;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.timetable.Trip;

public class TripMatcherFactoryTest {

  public static final FeedScopedId AKT_ID = new FeedScopedId("F", "AKT");
  public static final FeedScopedId RUTER1_ID = new FeedScopedId("F", "RUT:1");
  public static final FeedScopedId RUTER2_ID = new FeedScopedId("F", "RUT:2");
  private Trip tripRut;
  private Trip tripRut2;
  private Trip tripAkt;

  @BeforeEach
  void setup() {
    tripRut =
      Trip
        .of(new FeedScopedId("F", "RUT:route:trip:1"))
        .withRoute(
          Route
            .of(new FeedScopedId("F", "RUT:route:1"))
            .withAgency(Agency.of(RUTER1_ID).withName("RUT").withTimezone("Europe/Oslo").build())
            .withMode(TransitMode.BUS)
            .withShortName("BUS")
            .build()
        )
        .withServiceId(new FeedScopedId("F", "RUT:route:trip:1"))
        .build();
    tripRut2 =
      Trip
        .of(new FeedScopedId("F", "RUT:route:trip:2"))
        .withRoute(
          Route
            .of(new FeedScopedId("F", "RUT:route:2"))
            .withAgency(Agency.of(RUTER2_ID).withName("RUT").withTimezone("Europe/Oslo").build())
            .withMode(TransitMode.BUS)
            .withShortName("BUS")
            .build()
        )
        .withServiceId(new FeedScopedId("F", "RUT:route:trip:2"))
        .build();
    tripAkt =
      Trip
        .of(new FeedScopedId("F", "AKT:route:trip:1"))
        .withRoute(
          Route
            .of(new FeedScopedId("F", "AKT:route:1"))
            .withAgency(Agency.of(AKT_ID).withName("AKT").withTimezone("Europe/Oslo").build())
            .withMode(TransitMode.BUS)
            .withShortName("BUS")
            .build()
        )
        .withServiceId(new FeedScopedId("F", "AKT:route:trip:1"))
        .build();
  }

  @Test
  void testMatchIncludeRouteId() {
    TripRequest request = TripRequest
      .of()
      .withIncludedRoutes(
        FilterValues.ofEmptyIsEverything("routes", List.of(new FeedScopedId("F", "RUT:route:1")))
      )
      .build();

    Matcher<Trip> matcher = TripMatcherFactory.of(request, feedScopedId -> Set.of());

    assertTrue(matcher.match(tripRut));
    assertFalse(matcher.match(tripRut2));
    assertFalse(matcher.match(tripAkt));
  }

  @Test
  void testMatchExcludeRouteId() {
    TripRequest request = TripRequest
      .of()
      .withExcludedRoutes(
        FilterValues.ofEmptyIsEverything("routes", List.of(new FeedScopedId("F", "RUT:route:1")))
      )
      .build();

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
  void testMatchIncludeAgencyId() {
    TripRequest request = TripRequest
      .of()
      .withIncludedAgencies(
        FilterValues.ofEmptyIsEverything("agencies", List.of(new FeedScopedId("F", "RUT:1")))
      )
      .build();

    Matcher<Trip> matcher = TripMatcherFactory.of(request, feedScopedId -> Set.of());

    assertTrue(matcher.match(tripRut));
    assertFalse(matcher.match(tripRut2));
    assertFalse(matcher.match(tripAkt));
  }

  @Test
  void testMatchExcludeAgencyId() {
    TripRequest request = TripRequest
      .of()
      .withExcludedAgencies(FilterValues.ofEmptyIsEverything("agencies", List.of(RUTER1_ID)))
      .build();

    Matcher<Trip> matcher = TripMatcherFactory.of(request, feedScopedId -> Set.of());

    assertFalse(matcher.match(tripRut));
    assertTrue(matcher.match(tripRut2));
    assertTrue(matcher.match(tripAkt));
  }

  @Test
  void testIncludeNoAgencies() {
    TripRequest request = TripRequest
      .of()
      .withIncludedAgencies(FilterValues.ofEmptyIsNothing("agencies", List.of()))
      .build();

    Matcher<Trip> matcher = TripMatcherFactory.of(request, feedScopedId -> Set.of());

    assertFalse(matcher.match(tripRut));
    assertFalse(matcher.match(tripRut2));
    assertFalse(matcher.match(tripAkt));
  }

  @Test
  void testDisjointAgencyFilters() {
    TripRequest request = TripRequest
      .of()
      .withIncludedAgencies(FilterValues.ofEmptyIsNothing("agencies", List.of(RUTER1_ID)))
      .withExcludedAgencies(FilterValues.ofEmptyIsEverything("agencies", List.of(AKT_ID)))
      .build();

    Matcher<Trip> matcher = TripMatcherFactory.of(request, feedScopedId -> Set.of());

    assertTrue(matcher.match(tripRut));
    assertFalse(matcher.match(tripRut2));
    assertFalse(matcher.match(tripAkt));
  }

  @Test
  void testIntersectingAgencyFilters() {
    TripRequest request = TripRequest
      .of()
      .withIncludedAgencies(
        FilterValues.ofEmptyIsNothing("agencies", List.of(RUTER1_ID, RUTER2_ID, AKT_ID))
      )
      .withExcludedAgencies(FilterValues.ofEmptyIsEverything("agencies", List.of(AKT_ID)))
      .build();

    Matcher<Trip> matcher = TripMatcherFactory.of(request, feedScopedId -> Set.of());

    assertTrue(matcher.match(tripRut));
    assertTrue(matcher.match(tripRut2));
    assertFalse(matcher.match(tripAkt));
  }

  @Test
  void testMatchServiceDates() {
    TripRequest request = TripRequest
      .of()
      .withServiceDates(
        FilterValues.ofEmptyIsEverything(
          "operatingDays",
          List.of(LocalDate.of(2024, 2, 22), LocalDate.of(2024, 2, 23))
        )
      )
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
