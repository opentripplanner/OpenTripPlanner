package org.opentripplanner.transit.api.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.filter.transit.TripTimeOnDateFilterRequest;
import org.opentripplanner.transit.model.filter.transit.TripTimeOnDateSelectRequest;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.ArrivalDeparture;

class TripTimeOnDateRequestBuilderTest {

  private static final RegularStop STOP = TimetableRepositoryForTest.of().stop("1").build();
  private static final Instant TIME = Instant.parse("2025-03-02T10:00:00Z");

  @Test
  void defaults() {
    var request = TripTimeOnDateRequest.of(List.of(STOP)).withTime(TIME).build();

    assertEquals(TIME, request.time());
    assertEquals(Duration.ofHours(2), request.timeWindow());
    assertEquals(ArrivalDeparture.BOTH, request.arrivalDeparture());
    assertEquals(10, request.numberOfDepartures());
    assertFalse(request.includeCancelledTrips());
    assertTrue(request.includeAgencies().includeEverything());
    assertTrue(request.includeRoutes().includeEverything());
    assertTrue(request.excludeAgencies().includeEverything());
    assertTrue(request.excludeRoutes().includeEverything());
    assertTrue(request.includeModes().includeEverything());
    assertTrue(request.excludeModes().includeEverything());
    assertTrue(request.transitFilters().isEmpty());
    assertNotNull(request.sortOrder());
    assertTrue(request.stopLocations().contains(STOP));
  }

  @Test
  void withTimeWindow() {
    var request = TripTimeOnDateRequest.of(List.of(STOP))
      .withTime(TIME)
      .withTimeWindow(Duration.ofMinutes(30))
      .build();

    assertEquals(Duration.ofMinutes(30), request.timeWindow());
  }

  @Test
  void withArrivalDeparture() {
    var request = TripTimeOnDateRequest.of(List.of(STOP))
      .withTime(TIME)
      .withArrivalDeparture(ArrivalDeparture.DEPARTURES)
      .build();

    assertEquals(ArrivalDeparture.DEPARTURES, request.arrivalDeparture());
  }

  @Test
  void withNumberOfDepartures() {
    var request = TripTimeOnDateRequest.of(List.of(STOP))
      .withTime(TIME)
      .withNumberOfDepartures(5)
      .build();

    assertEquals(5, request.numberOfDepartures());
  }

  @Test
  void withIncludeCancelledTrips() {
    var request = TripTimeOnDateRequest.of(List.of(STOP))
      .withTime(TIME)
      .withIncludeCancelledTrips(true)
      .build();

    assertTrue(request.includeCancelledTrips());
  }

  @Test
  void withIncludeAgencies() {
    var agencyId = new FeedScopedId("F", "A1");
    var request = TripTimeOnDateRequest.of(List.of(STOP))
      .withTime(TIME)
      .withIncludeAgencies(List.of(agencyId))
      .build();

    assertFalse(request.includeAgencies().includeEverything());
    assertTrue(request.includeAgencies().get().contains(agencyId));
  }

  @Test
  void withIncludeRoutes() {
    var routeId = new FeedScopedId("F", "R1");
    var request = TripTimeOnDateRequest.of(List.of(STOP))
      .withTime(TIME)
      .withIncludeRoutes(List.of(routeId))
      .build();

    assertFalse(request.includeRoutes().includeEverything());
    assertTrue(request.includeRoutes().get().contains(routeId));
  }

  @Test
  void withExcludeAgencies() {
    var agencyId = new FeedScopedId("F", "A1");
    var request = TripTimeOnDateRequest.of(List.of(STOP))
      .withTime(TIME)
      .withExcludeAgencies(List.of(agencyId))
      .build();

    assertFalse(request.excludeAgencies().includeEverything());
    assertTrue(request.excludeAgencies().get().contains(agencyId));
  }

  @Test
  void withExcludeRoutes() {
    var routeId = new FeedScopedId("F", "R1");
    var request = TripTimeOnDateRequest.of(List.of(STOP))
      .withTime(TIME)
      .withExcludeRoutes(List.of(routeId))
      .build();

    assertFalse(request.excludeRoutes().includeEverything());
    assertTrue(request.excludeRoutes().get().contains(routeId));
  }

  @Test
  void withIncludeModes() {
    var request = TripTimeOnDateRequest.of(List.of(STOP))
      .withTime(TIME)
      .withIncludeModes(List.of(TransitMode.BUS))
      .build();

    assertFalse(request.includeModes().includeEverything());
    assertTrue(request.includeModes().get().contains(TransitMode.BUS));
  }

  @Test
  void withExcludeModes() {
    var request = TripTimeOnDateRequest.of(List.of(STOP))
      .withTime(TIME)
      .withExcludeModes(List.of(TransitMode.RAIL))
      .build();

    assertFalse(request.excludeModes().includeEverything());
    assertTrue(request.excludeModes().get().contains(TransitMode.RAIL));
  }

  @Test
  void withNullIncludeAndExcludes() {
    var request = TripTimeOnDateRequest.of(List.of(STOP))
      .withTime(TIME)
      .withIncludeRoutes(null)
      .withExcludeRoutes(null)
      .withIncludeAgencies(null)
      .withExcludeAgencies(null)
      .withIncludeModes(null)
      .withExcludeModes(null)
      .build();

    assertTrue(request.includeRoutes().includeEverything());
    assertTrue(request.includeAgencies().includeEverything());
    assertTrue(request.includeModes().includeEverything());
    assertTrue(request.excludeRoutes().includeEverything());
    assertTrue(request.excludeAgencies().includeEverything());
    assertTrue(request.excludeModes().includeEverything());
  }

  @Test
  void withTransitFilters() {
    var filter = TripTimeOnDateFilterRequest.of()
      .addSelect(
        TripTimeOnDateSelectRequest.of().withAgencies(List.of(new FeedScopedId("F", "A1"))).build()
      )
      .build();

    var request = TripTimeOnDateRequest.of(List.of(STOP))
      .withTime(TIME)
      .withTransitFilters(List.of(filter))
      .build();

    assertEquals(1, request.transitFilters().size());
  }
}
