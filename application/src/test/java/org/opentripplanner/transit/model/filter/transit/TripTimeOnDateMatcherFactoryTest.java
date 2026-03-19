package org.opentripplanner.transit.model.filter.transit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.agency;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.route;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.api.request.TripTimeOnDateRequest;
import org.opentripplanner.transit.api.request.TripTimeOnDateRequestBuilder;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.ScheduledTripTimes;

class TripTimeOnDateMatcherFactoryTest {

  private static final RegularStop STOP = TimetableRepositoryForTest.of().stop("1").build();
  private static final LocalDate DATE = LocalDate.of(2025, 3, 2);

  private static final Route ROUTE_1 = route("r1")
    .withAgency(agency("a1"))
    .withMode(TransitMode.RAIL)
    .build();
  private static final Route ROUTE_2 = route("r2")
    .withAgency(agency("a2"))
    .withMode(TransitMode.BUS)
    .build();
  private static final Route ROUTE_3 = route("r2")
    .withAgency(agency("a3"))
    .withMode(TransitMode.FERRY)
    .build();

  @Test
  void noFilters() {
    var request = request().build();

    var matcher = TripTimeOnDateMatcherFactory.of(request);

    assertTrue(matcher.match(tripTimeOnDate(ROUTE_1)));
  }

  @Test
  void includeRoute() {
    var request = request().withIncludeRoutes(List.of(ROUTE_1.getId())).build();

    var matcher = TripTimeOnDateMatcherFactory.of(request);

    assertTrue(matcher.match(tripTimeOnDate(ROUTE_1)));
    assertFalse(matcher.match(tripTimeOnDate(ROUTE_2)));
  }

  @Test
  void excludeRoute() {
    var request = request().withExcludeRoutes(List.of(ROUTE_1.getId())).build();

    var matcher = TripTimeOnDateMatcherFactory.of(request);

    assertFalse(matcher.match(tripTimeOnDate(ROUTE_1)));
    assertTrue(matcher.match(tripTimeOnDate(ROUTE_2)));
  }

  @Test
  void includeAgency() {
    var request = request().withIncludeAgencies(List.of(ROUTE_1.getAgency().getId())).build();

    var matcher = TripTimeOnDateMatcherFactory.of(request);

    assertTrue(matcher.match(tripTimeOnDate(ROUTE_1)));
    assertFalse(matcher.match(tripTimeOnDate(ROUTE_2)));
  }

  @Test
  void excludeAgency() {
    var request = request().withExcludeAgencies(List.of(ROUTE_1.getAgency().getId())).build();

    var matcher = TripTimeOnDateMatcherFactory.of(request);

    assertFalse(matcher.match(tripTimeOnDate(ROUTE_1)));
    assertTrue(matcher.match(tripTimeOnDate(ROUTE_2)));
  }

  @Test
  void includeMode() {
    var request = request().withIncludeModes(List.of(ROUTE_1.getMode())).build();

    var matcher = TripTimeOnDateMatcherFactory.of(request);

    assertTrue(matcher.match(tripTimeOnDate(ROUTE_1)));
    assertFalse(matcher.match(tripTimeOnDate(ROUTE_2)));
  }

  @Test
  void excludeMode() {
    var request = request().withExcludeModes(List.of(ROUTE_1.getMode())).build();

    var matcher = TripTimeOnDateMatcherFactory.of(request);

    assertFalse(matcher.match(tripTimeOnDate(ROUTE_1)));
    assertTrue(matcher.match(tripTimeOnDate(ROUTE_2)));
  }

  @Test
  void excludeModeAndRoute() {
    var request = request()
      .withExcludeModes(List.of(ROUTE_1.getMode()))
      .withExcludeRoutes(List.of(ROUTE_2.getId()))
      .build();

    var matcher = TripTimeOnDateMatcherFactory.of(request);

    assertFalse(matcher.match(tripTimeOnDate(ROUTE_1)));
    assertFalse(matcher.match(tripTimeOnDate(ROUTE_2)));
  }

  @Test
  void excludeAgencyAndRoute() {
    var request = request()
      .withExcludeModes(List.of(ROUTE_1.getMode()))
      .withExcludeAgencies(List.of(ROUTE_2.getAgency().getId()))
      .build();

    var matcher = TripTimeOnDateMatcherFactory.of(request);

    assertFalse(matcher.match(tripTimeOnDate(ROUTE_1)));
    assertFalse(matcher.match(tripTimeOnDate(ROUTE_2)));
    assertTrue(matcher.match(tripTimeOnDate(ROUTE_3)));
  }

  @Nested
  class SelectBasedFilters {

    @Test
    void selectByAgency() {
      var filter = TripTimeOnDateFilterRequest.of()
        .addSelect(
          TripTimeOnDateSelectRequest.of()
            .withAgencies(List.of(ROUTE_1.getAgency().getId()))
            .build()
        )
        .build();

      var matcher = TripTimeOnDateMatcherFactory.ofSelectorBasedTransitFilters(List.of(filter));

      assertTrue(matcher.match(tripTimeOnDate(ROUTE_1)));
      assertFalse(matcher.match(tripTimeOnDate(ROUTE_2)));
    }

    @Test
    void selectByRoute() {
      var filter = TripTimeOnDateFilterRequest.of()
        .addSelect(TripTimeOnDateSelectRequest.of().withRoutes(List.of(ROUTE_1.getId())).build())
        .build();

      var matcher = TripTimeOnDateMatcherFactory.ofSelectorBasedTransitFilters(List.of(filter));

      assertTrue(matcher.match(tripTimeOnDate(ROUTE_1)));
      assertFalse(matcher.match(tripTimeOnDate(ROUTE_2)));
    }

    @Test
    void selectByMode() {
      var filter = TripTimeOnDateFilterRequest.of()
        .addSelect(
          TripTimeOnDateSelectRequest.of()
            .withTransportModes(List.of(new MainAndSubMode(TransitMode.RAIL)))
            .build()
        )
        .build();

      var matcher = TripTimeOnDateMatcherFactory.ofSelectorBasedTransitFilters(List.of(filter));

      assertTrue(matcher.match(tripTimeOnDate(ROUTE_1)));
      assertFalse(matcher.match(tripTimeOnDate(ROUTE_2)));
    }

    @Test
    void notByAgency() {
      var filter = TripTimeOnDateFilterRequest.of()
        .addNot(
          TripTimeOnDateSelectRequest.of()
            .withAgencies(List.of(ROUTE_1.getAgency().getId()))
            .build()
        )
        .build();

      var matcher = TripTimeOnDateMatcherFactory.ofSelectorBasedTransitFilters(List.of(filter));

      assertFalse(matcher.match(tripTimeOnDate(ROUTE_1)));
      assertTrue(matcher.match(tripTimeOnDate(ROUTE_2)));
    }

    @Test
    void notByRoute() {
      var filter = TripTimeOnDateFilterRequest.of()
        .addNot(TripTimeOnDateSelectRequest.of().withRoutes(List.of(ROUTE_2.getId())).build())
        .build();

      var matcher = TripTimeOnDateMatcherFactory.ofSelectorBasedTransitFilters(List.of(filter));

      assertTrue(matcher.match(tripTimeOnDate(ROUTE_1)));
      assertFalse(matcher.match(tripTimeOnDate(ROUTE_2)));
    }

    @Test
    void notByMode() {
      var filter = TripTimeOnDateFilterRequest.of()
        .addNot(
          TripTimeOnDateSelectRequest.of()
            .withTransportModes(List.of(new MainAndSubMode(TransitMode.BUS)))
            .build()
        )
        .build();

      var matcher = TripTimeOnDateMatcherFactory.ofSelectorBasedTransitFilters(List.of(filter));

      assertTrue(matcher.match(tripTimeOnDate(ROUTE_1)));
      assertFalse(matcher.match(tripTimeOnDate(ROUTE_2)));
    }

    @Test
    void selectAndNot() {
      // Select RAIL and BUS, but exclude agency a1 (RAIL)
      var filter = TripTimeOnDateFilterRequest.of()
        .addSelect(
          TripTimeOnDateSelectRequest.of()
            .withTransportModes(
              List.of(new MainAndSubMode(TransitMode.RAIL), new MainAndSubMode(TransitMode.BUS))
            )
            .build()
        )
        .addNot(
          TripTimeOnDateSelectRequest.of()
            .withAgencies(List.of(ROUTE_1.getAgency().getId()))
            .build()
        )
        .build();

      var matcher = TripTimeOnDateMatcherFactory.ofSelectorBasedTransitFilters(List.of(filter));

      // ROUTE_1 is RAIL by agency a1 -> matches select but also matches not -> excluded
      assertFalse(matcher.match(tripTimeOnDate(ROUTE_1)));
      // ROUTE_2 is BUS by agency a2 -> matches select, doesn't match not -> included
      assertTrue(matcher.match(tripTimeOnDate(ROUTE_2)));
      // ROUTE_3 is FERRY by agency a3 -> doesn't match select -> excluded
      assertFalse(matcher.match(tripTimeOnDate(ROUTE_3)));
    }

    @Test
    void multipleSelectsOrBetween() {
      // Two select criteria: agency a1 OR agency a3
      var filter = TripTimeOnDateFilterRequest.of()
        .addSelect(
          TripTimeOnDateSelectRequest.of()
            .withAgencies(List.of(ROUTE_1.getAgency().getId()))
            .build()
        )
        .addSelect(
          TripTimeOnDateSelectRequest.of()
            .withAgencies(List.of(ROUTE_3.getAgency().getId()))
            .build()
        )
        .build();

      var matcher = TripTimeOnDateMatcherFactory.ofSelectorBasedTransitFilters(List.of(filter));

      assertTrue(matcher.match(tripTimeOnDate(ROUTE_1)));
      assertFalse(matcher.match(tripTimeOnDate(ROUTE_2)));
      assertTrue(matcher.match(tripTimeOnDate(ROUTE_3)));
    }

    @Test
    void selectWithAgencyAndMode() {
      // Select items where agency is a1 AND mode is RAIL
      var filter = TripTimeOnDateFilterRequest.of()
        .addSelect(
          TripTimeOnDateSelectRequest.of()
            .withAgencies(List.of(ROUTE_1.getAgency().getId()))
            .withTransportModes(List.of(new MainAndSubMode(TransitMode.RAIL)))
            .build()
        )
        .build();

      var matcher = TripTimeOnDateMatcherFactory.ofSelectorBasedTransitFilters(List.of(filter));

      // ROUTE_1 is RAIL by agency a1 -> matches both -> included
      assertTrue(matcher.match(tripTimeOnDate(ROUTE_1)));
      // ROUTE_2 is BUS by agency a2 -> doesn't match -> excluded
      assertFalse(matcher.match(tripTimeOnDate(ROUTE_2)));
    }

    @Test
    void multipleFiltersOrBetween() {
      // Two filters: filter1 selects agency a1, filter2 selects agency a2
      // OR between filters
      var filter1 = TripTimeOnDateFilterRequest.of()
        .addSelect(
          TripTimeOnDateSelectRequest.of()
            .withAgencies(List.of(ROUTE_1.getAgency().getId()))
            .build()
        )
        .build();
      var filter2 = TripTimeOnDateFilterRequest.of()
        .addSelect(
          TripTimeOnDateSelectRequest.of()
            .withAgencies(List.of(ROUTE_2.getAgency().getId()))
            .build()
        )
        .build();

      var matcher = TripTimeOnDateMatcherFactory.ofSelectorBasedTransitFilters(
        List.of(filter1, filter2)
      );

      assertTrue(matcher.match(tripTimeOnDate(ROUTE_1)));
      assertTrue(matcher.match(tripTimeOnDate(ROUTE_2)));
      assertFalse(matcher.match(tripTimeOnDate(ROUTE_3)));
    }

    @Test
    void notWithAgencyAndMode() {
      var filter = TripTimeOnDateFilterRequest.of()
        .addNot(
          TripTimeOnDateSelectRequest.of()
            .withAgencies(List.of(ROUTE_1.getAgency().getId()))
            .withTransportModes(List.of(new MainAndSubMode(TransitMode.RAIL)))
            .build()
        )
        .build();

      var matcher = TripTimeOnDateMatcherFactory.ofSelectorBasedTransitFilters(List.of(filter));

      // ROUTE_1 matches (RAIL and agency a1), so it is excluded
      assertFalse(matcher.match(tripTimeOnDate(ROUTE_1)));
      // ROUTE_2 and ROUTE_3 do not, so both are included
      assertTrue(matcher.match(tripTimeOnDate(ROUTE_2)));
      assertTrue(matcher.match(tripTimeOnDate(ROUTE_3)));
    }

    @Test
    void multipleNotsInFilter() {
      var filter = TripTimeOnDateFilterRequest.of()
        .addNot(
          TripTimeOnDateSelectRequest.of()
            .withAgencies(List.of(ROUTE_1.getAgency().getId()))
            .build()
        )
        .addNot(
          TripTimeOnDateSelectRequest.of()
            .withTransportModes(List.of(new MainAndSubMode(TransitMode.BUS)))
            .build()
        )
        .build();

      var matcher = TripTimeOnDateMatcherFactory.ofSelectorBasedTransitFilters(List.of(filter));

      // ROUTE_1 is RAIL -> excluded
      assertFalse(matcher.match(tripTimeOnDate(ROUTE_1)));
      // ROUTE_2 is agency a2 -> excluded
      assertFalse(matcher.match(tripTimeOnDate(ROUTE_2)));
      // ROUTE_3 does not match any not's so it is included
      assertTrue(matcher.match(tripTimeOnDate(ROUTE_3)));
    }

    @Test
    void selectWithMultipleAgencies() {
      var filter = TripTimeOnDateFilterRequest.of()
        .addSelect(
          TripTimeOnDateSelectRequest.of()
            .withAgencies(List.of(ROUTE_1.getAgency().getId(), ROUTE_2.getAgency().getId()))
            .build()
        )
        .build();

      var matcher = TripTimeOnDateMatcherFactory.ofSelectorBasedTransitFilters(List.of(filter));

      assertTrue(matcher.match(tripTimeOnDate(ROUTE_1)));
      assertTrue(matcher.match(tripTimeOnDate(ROUTE_2)));
      assertFalse(matcher.match(tripTimeOnDate(ROUTE_3)));
    }

    @Test
    void multipleFiltersSelectAndNot() {
      var filter1 = TripTimeOnDateFilterRequest.of()
        .addSelect(
          TripTimeOnDateSelectRequest.of()
            .withAgencies(List.of(ROUTE_1.getAgency().getId()))
            .build()
        )
        .build();
      var filter2 = TripTimeOnDateFilterRequest.of()
        .addNot(
          TripTimeOnDateSelectRequest.of()
            .withAgencies(List.of(ROUTE_2.getAgency().getId()))
            .build()
        )
        .build();

      var matcher = TripTimeOnDateMatcherFactory.ofSelectorBasedTransitFilters(
        List.of(filter1, filter2)
      );

      // ROUTE_1 is agency a1 -> matches filter1
      assertTrue(matcher.match(tripTimeOnDate(ROUTE_1)));
      // ROUTE_2 a2 is not included in filter1 and excluded in filter2
      assertFalse(matcher.match(tripTimeOnDate(ROUTE_2)));
      // ROUTE_3 a3 is not excluded in filter2
      assertTrue(matcher.match(tripTimeOnDate(ROUTE_3)));
    }

    @Test
    void multipleFiltersWithSelectNotAndSelect() {
      var filter1 = TripTimeOnDateFilterRequest.of()
        .addSelect(
          TripTimeOnDateSelectRequest.of()
            .withTransportModes(List.of(new MainAndSubMode(TransitMode.RAIL)))
            .build()
        )
        .addNot(
          TripTimeOnDateSelectRequest.of()
            .withAgencies(List.of(ROUTE_1.getAgency().getId()))
            .build()
        )
        .build();
      var filter2 = TripTimeOnDateFilterRequest.of()
        .addSelect(
          TripTimeOnDateSelectRequest.of()
            .withTransportModes(List.of(new MainAndSubMode(TransitMode.FERRY)))
            .build()
        )
        .build();

      var matcher = TripTimeOnDateMatcherFactory.ofSelectorBasedTransitFilters(
        List.of(filter1, filter2)
      );

      // ROUTE_1 is RAIL by a1, selected in filter1 but excluded by not -> no match in filter1, not FERRY -> no match in filter2
      assertFalse(matcher.match(tripTimeOnDate(ROUTE_1)));
      // ROUTE_2 is BUS, not selected in filter1, not FERRY -> no match in filter2
      assertFalse(matcher.match(tripTimeOnDate(ROUTE_2)));
      // ROUTE_3 is FERRY -> matches filter2
      assertTrue(matcher.match(tripTimeOnDate(ROUTE_3)));
    }

    @Test
    void emptyFilterMatchesEverything() {
      // Empty filter (no select, no not) -> matches everything
      var filter = TripTimeOnDateFilterRequest.of().build();

      var matcher = TripTimeOnDateMatcherFactory.ofSelectorBasedTransitFilters(List.of(filter));

      assertTrue(matcher.match(tripTimeOnDate(ROUTE_1)));
      assertTrue(matcher.match(tripTimeOnDate(ROUTE_2)));
      assertTrue(matcher.match(tripTimeOnDate(ROUTE_3)));
    }

    @Test
    void emptyFilterListMatchesNothing() {
      var matcher = TripTimeOnDateMatcherFactory.ofSelectorBasedTransitFilters(List.of());

      assertFalse(matcher.match(tripTimeOnDate(ROUTE_1)));
    }

    @Test
    void emptySelectSelectorMatchesEverything() {
      // An empty selector matches everything
      var filter = TripTimeOnDateFilterRequest.of()
        .addSelect(TripTimeOnDateSelectRequest.of().build())
        .build();

      var matcher = TripTimeOnDateMatcherFactory.ofSelectorBasedTransitFilters(List.of(filter));

      assertTrue(matcher.match(tripTimeOnDate(ROUTE_1)));
      assertTrue(matcher.match(tripTimeOnDate(ROUTE_2)));
    }

    @Test
    void emptyNotSelectorMatchesNothing() {
      // An empty not-selector matches everything, so everything is excluded
      var filter = TripTimeOnDateFilterRequest.of()
        .addNot(TripTimeOnDateSelectRequest.of().build())
        .build();

      var matcher = TripTimeOnDateMatcherFactory.ofSelectorBasedTransitFilters(List.of(filter));

      assertFalse(matcher.match(tripTimeOnDate(ROUTE_1)));
      assertFalse(matcher.match(tripTimeOnDate(ROUTE_2)));
    }

    @Test
    void emptySelectAndEmptyNotMatchesNothing() {
      // Empty select matches everything, empty not excludes everything -> nothing
      var filter = TripTimeOnDateFilterRequest.of()
        .addSelect(TripTimeOnDateSelectRequest.of().build())
        .addNot(TripTimeOnDateSelectRequest.of().build())
        .build();

      var matcher = TripTimeOnDateMatcherFactory.ofSelectorBasedTransitFilters(List.of(filter));

      assertFalse(matcher.match(tripTimeOnDate(ROUTE_1)));
      assertFalse(matcher.match(tripTimeOnDate(ROUTE_2)));
    }

    @Test
    void transitFiltersViaRequest() {
      // Test that transit filters work when set through the request builder
      var filter = TripTimeOnDateFilterRequest.of()
        .addNot(
          TripTimeOnDateSelectRequest.of()
            .withAgencies(List.of(ROUTE_1.getAgency().getId()))
            .build()
        )
        .build();

      var matcherRequest = request().withTransitFilters(List.of(filter)).build();
      var matcher = TripTimeOnDateMatcherFactory.of(matcherRequest);

      assertFalse(matcher.match(tripTimeOnDate(ROUTE_1)));
      assertTrue(matcher.match(tripTimeOnDate(ROUTE_2)));
    }

    @Test
    void selectorSelectAndFlatExcludeMode() {
      var filter = TripTimeOnDateFilterRequest.of()
        .addSelect(
          TripTimeOnDateSelectRequest.of()
            .withAgencies(List.of(ROUTE_1.getAgency().getId()))
            .build()
        )
        .build();

      var matcherRequest = request()
        .withTransitFilters(List.of(filter))
        .withExcludeModes(List.of(ROUTE_1.getMode()))
        .build();
      var matcher = TripTimeOnDateMatcherFactory.of(matcherRequest);

      // ROUTE_1: selected by filter but excluded by flat excludeMode
      assertFalse(matcher.match(tripTimeOnDate(ROUTE_1)));
      // ROUTE_2: not selected by filter
      assertFalse(matcher.match(tripTimeOnDate(ROUTE_2)));
    }

    @Test
    void selectorSelectAndFlatIncludeAgency() {
      // Selector selects RAIL, flat filter includes only agency a2
      var filter = TripTimeOnDateFilterRequest.of()
        .addSelect(
          TripTimeOnDateSelectRequest.of()
            .withTransportModes(List.of(new MainAndSubMode(TransitMode.RAIL)))
            .build()
        )
        .build();

      var matcherRequest = request()
        .withTransitFilters(List.of(filter))
        .withIncludeAgencies(List.of(ROUTE_2.getAgency().getId()))
        .build();
      var matcher = TripTimeOnDateMatcherFactory.of(matcherRequest);

      // ROUTE_1: RAIL -> selected by filter, but agency a1 not in flat include -> excluded
      assertFalse(matcher.match(tripTimeOnDate(ROUTE_1)));
      // ROUTE_2: BUS -> not selected by filter -> excluded
      assertFalse(matcher.match(tripTimeOnDate(ROUTE_2)));
    }

    @Test
    void selectorNotAndFlatIncludeRoute() {
      // Selector excludes agency a1, flat filter includes only ROUTE_1
      // Both must pass (AND), so ROUTE_1 is excluded despite being in flat include
      var filter = TripTimeOnDateFilterRequest.of()
        .addNot(
          TripTimeOnDateSelectRequest.of()
            .withAgencies(List.of(ROUTE_1.getAgency().getId()))
            .build()
        )
        .build();

      var matcherRequest = request()
        .withTransitFilters(List.of(filter))
        .withIncludeRoutes(List.of(ROUTE_1.getId()))
        .build();
      var matcher = TripTimeOnDateMatcherFactory.of(matcherRequest);

      // ROUTE_1: passes flat includeRoute, but excluded by selector-not -> excluded
      assertFalse(matcher.match(tripTimeOnDate(ROUTE_1)));
      // ROUTE_2: passes selector-not, but not in flat includeRoute -> excluded
      assertFalse(matcher.match(tripTimeOnDate(ROUTE_2)));
    }

    @Test
    void selectorSelectAndFlatExcludeAgency() {
      // Selector selects agencies a1 and a2, flat filter excludes agency a1
      var filter = TripTimeOnDateFilterRequest.of()
        .addSelect(
          TripTimeOnDateSelectRequest.of()
            .withAgencies(List.of(ROUTE_1.getAgency().getId(), ROUTE_2.getAgency().getId()))
            .build()
        )
        .build();

      var matcherRequest = request()
        .withTransitFilters(List.of(filter))
        .withExcludeAgencies(List.of(ROUTE_1.getAgency().getId()))
        .build();
      var matcher = TripTimeOnDateMatcherFactory.of(matcherRequest);

      // ROUTE_1: selected by filter, but excluded by flat excludeAgency
      assertFalse(matcher.match(tripTimeOnDate(ROUTE_1)));
      // ROUTE_2: selected by filter, not excluded by flat
      assertTrue(matcher.match(tripTimeOnDate(ROUTE_2)));
      // ROUTE_3: not selected by filter
      assertFalse(matcher.match(tripTimeOnDate(ROUTE_3)));
    }
  }

  private static TripTimeOnDateRequestBuilder request() {
    return TripTimeOnDateRequest.of(List.of(STOP)).withTime(Instant.EPOCH);
  }

  private static TripPattern pattern(Route route) {
    return TimetableRepositoryForTest.tripPattern("p1", route)
      .withStopPattern(TimetableRepositoryForTest.stopPattern(STOP, STOP))
      .build();
  }

  private static TripTimeOnDate tripTimeOnDate(Route route1) {
    final TripPattern pattern = pattern(route1);
    var tripTimes = ScheduledTripTimes.of()
      .withTrip(TimetableRepositoryForTest.trip("t1").withRoute(route1).build())
      .withArrivalTimes("10:00 10:05")
      .withDepartureTimes("10:00 10:05")
      .build();
    return new TripTimeOnDate(tripTimes, 0, pattern, DATE, Instant.EPOCH);
  }
}
