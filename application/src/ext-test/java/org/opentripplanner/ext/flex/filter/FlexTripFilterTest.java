package org.opentripplanner.ext.flex.filter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.of;
import static org.opentripplanner.ext.flex.filter.FlexTripFilterTest.FilterResult.EXCLUDED;
import static org.opentripplanner.ext.flex.filter.FlexTripFilterTest.FilterResult.SELECTED;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.agency;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.ext.flex.filter.FlexTripFilterRequest.Filter;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.timetable.Trip;

class FlexTripFilterTest {

  private static List<Arguments> allowAllCases() {
    return List.of(
      of(route("r1"), agency("a1")),
      of(route("r1"), agency("a2")),
      of(route("r2"), agency("a2")),
      of(route("r3"), agency("a3"))
    );
  }

  @ParameterizedTest
  @MethodSource("allowAllCases")
  void allowAll(Route route, Agency agency) {
    var filter = FlexTripFilter.ALLOW_ALL;

    var trip = trip(route, agency);

    assertTrue(filter.allowsTrip(trip));
  }

  private static List<Arguments> selectedAgencyCases() {
    return List.of(
      of(route("r1"), agency("a1"), EXCLUDED),
      of(route("r1"), agency("a2"), EXCLUDED),
      of(route("selected"), agency("a2"), EXCLUDED),
      of(route("selected"), agency("selected"), SELECTED),
      of(route("r1"), agency("selected"), SELECTED)
    );
  }

  @ParameterizedTest
  @MethodSource("selectedAgencyCases")
  void selectedAgency(Route route, Agency agency, FilterResult expectation) {
    var filter = new FlexTripFilter(
      List.of(new Filter(Set.of(id("selected")), Set.of(), Set.of(), Set.of()))
    );

    var trip = trip(route, agency);

    expectation.assertFilter(trip, filter);
  }

  private static List<Arguments> selectedRouteCases() {
    return List.of(
      of(route("r1"), agency("a1"), EXCLUDED),
      of(route("r1"), agency("a2"), EXCLUDED),
      of(route("selected"), agency("a2"), SELECTED),
      of(route("selected"), agency("selected"), SELECTED),
      of(route("r1"), agency("selected"), EXCLUDED)
    );
  }

  @ParameterizedTest
  @MethodSource("selectedRouteCases")
  void selectedRoute(Route route, Agency agency, FilterResult expectation) {
    var filter = new FlexTripFilter(
      List.of(new Filter(Set.of(), Set.of(), Set.of(id("selected")), Set.of()))
    );

    var trip = trip(route, agency);

    expectation.assertFilter(trip, filter);
  }

  private static List<Arguments> excludedAgencyCases() {
    return List.of(
      of(route("r1"), agency("a1"), SELECTED),
      of(route("r1"), agency("a2"), SELECTED),
      of(route("selected"), agency("a2"), SELECTED),
      of(route("excluded"), agency("excluded"), EXCLUDED),
      of(route("r1"), agency("excluded"), EXCLUDED)
    );
  }

  @ParameterizedTest
  @MethodSource("excludedAgencyCases")
  void excludedAgency(Route route, Agency agency, FilterResult expectation) {
    var filter = new FlexTripFilter(
      List.of(new Filter(Set.of(), Set.of(id("excluded")), Set.of(), Set.of()))
    );

    var trip = trip(route, agency);

    expectation.assertFilter(trip, filter);
  }

  private static List<Arguments> excludedRouteCases() {
    return List.of(
      of(route("r1"), agency("a1"), SELECTED),
      of(route("r1"), agency("a2"), SELECTED),
      of(route("selected"), agency("a2"), SELECTED),
      of(route("excluded"), agency("selected"), EXCLUDED),
      of(route("r1"), agency("excluded"), SELECTED)
    );
  }

  @ParameterizedTest
  @MethodSource("excludedRouteCases")
  void excludedRoute(Route route, Agency agency, FilterResult expectation) {
    var filter = new FlexTripFilter(
      List.of(new Filter(Set.of(), Set.of(), Set.of(), Set.of(id("excluded"))))
    );

    var trip = trip(route, agency);

    expectation.assertFilter(trip, filter);
  }

  private static List<Arguments> excludedCases() {
    return List.of(
      of(route("r1"), agency("a1"), SELECTED),
      of(route("r1"), agency("a2"), SELECTED),
      of(route("selected"), agency("a2"), SELECTED),
      of(route("excluded-route"), agency("a2"), EXCLUDED),
      of(route("excluded-route"), agency("excluded-agency"), EXCLUDED),
      of(route("r2"), agency("excluded-agency"), EXCLUDED)
    );
  }

  @ParameterizedTest
  @MethodSource("excludedCases")
  void excluded(Route route, Agency agency, FilterResult expectation) {
    var filter = new FlexTripFilter(
      List.of(
        new Filter(Set.of(), Set.of(id("excluded-agency")), Set.of(), Set.of(id("excluded-route")))
      )
    );

    var trip = trip(route, agency);

    expectation.assertFilter(trip, filter);
  }

  private static Trip trip(Route route, Agency agency) {
    var r = route.copy().withAgency(agency).build();
    return TimetableRepositoryForTest.trip("1").withRoute(r).build();
  }

  private static Route route(String routeId) {
    return TimetableRepositoryForTest.route(routeId).build();
  }

  enum FilterResult {
    SELECTED,
    EXCLUDED;

    void assertFilter(Trip trip, FlexTripFilter filter) {
      if (this == EXCLUDED) {
        Assertions.assertFalse(filter.allowsTrip(trip));
      } else if (this == SELECTED) {
        assertTrue(filter.allowsTrip(trip));
      }
    }
  }
}
