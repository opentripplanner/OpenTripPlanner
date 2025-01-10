package org.opentripplanner.transit.model.filter.transit;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.transit.api.request.FindRoutesRequest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.filter.expr.Matcher;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;

class RouteMatcherFactoryTest {

  private Route route1;
  private Route route2;

  @BeforeEach
  void setUp() {
    route1 =
      Route
        .of(new FeedScopedId("feedId", "routeId"))
        .withAgency(
          Agency
            .of(new FeedScopedId("feedId", "agencyId"))
            .withName("AGENCY")
            .withTimezone("Europe/Oslo")
            .build()
        )
        .withMode(TransitMode.BUS)
        .withShortName("ROUTE1")
        .withLongName(
          new I18NString() {
            @Override
            public String toString() {
              return "ROUTE1LONG";
            }

            @Override
            public String toString(Locale locale) {
              return "ROUTE1LONG";
            }
          }
        )
        .build();
    route2 =
      Route
        .of(new FeedScopedId("otherFeedId", "otherRouteId"))
        .withAgency(
          Agency
            .of(new FeedScopedId("otherFeedId", "otherAgencyId"))
            .withName("OTHER_AGENCY")
            .withTimezone("Europe/Oslo")
            .build()
        )
        .withMode(TransitMode.RAIL)
        .withShortName("ROUTE2")
        .withLongName(
          new I18NString() {
            @Override
            public String toString() {
              return "ROUTE2LONG";
            }

            @Override
            public String toString(Locale locale) {
              return "ROUTE2LONG";
            }
          }
        )
        .build();
  }

  @Test
  void testAgencies() {
    FindRoutesRequest request = FindRoutesRequest
      .of()
      .withAgencies(FilterValues.ofEmptyIsEverything("agencies", List.of("agencyId")))
      .build();

    Matcher<Route> matcher = RouteMatcherFactory.of(request, r -> false);
    assertTrue(matcher.match(route1));
    assertFalse(matcher.match(route2));
  }

  @Test
  void testTransitModes() {
    FindRoutesRequest request = FindRoutesRequest
      .of()
      .withTransitModes(FilterValues.ofEmptyIsEverything("transitModes", List.of(TransitMode.BUS)))
      .build();

    Matcher<Route> matcher = RouteMatcherFactory.of(request, r -> false);
    assertTrue(matcher.match(route1));
    assertFalse(matcher.match(route2));
  }

  @Test
  void testShortLongName() {
    FindRoutesRequest request = FindRoutesRequest.of().withShortName("ROUTE1").build();

    Matcher<Route> matcher = RouteMatcherFactory.of(request, r -> false);
    assertTrue(matcher.match(route1));
    assertFalse(matcher.match(route2));
  }

  @Test
  void testShortNames() {
    FindRoutesRequest request = FindRoutesRequest
      .of()
      .withShortNames(FilterValues.ofEmptyIsEverything("publicCodes", List.of("ROUTE1", "ROUTE3")))
      .build();

    Matcher<Route> matcher = RouteMatcherFactory.of(request, r -> false);
    assertTrue(matcher.match(route1));
    assertFalse(matcher.match(route2));
  }

  @Test
  void testIsFlexRoute() {
    FindRoutesRequest request = FindRoutesRequest.of().withFlexibleOnly(true).build();

    Set<Route> flexRoutes = Set.of(route1);

    Matcher<Route> matcher = RouteMatcherFactory.of(request, flexRoutes::contains);
    assertTrue(matcher.match(route1));
    assertFalse(matcher.match(route2));
  }

  @Test
  void testLongNameExactMatch() {
    FindRoutesRequest request = FindRoutesRequest.of().withLongName("ROUTE1LONG").build();

    Matcher<Route> matcher = RouteMatcherFactory.of(request, r -> false);
    assertTrue(matcher.match(route1));
    assertFalse(matcher.match(route2));
  }

  @Test
  void testLongNamePrefixMatch() {
    FindRoutesRequest request = FindRoutesRequest.of().withLongName("ROUTE1").build();

    Matcher<Route> matcher = RouteMatcherFactory.of(request, r -> false);
    assertTrue(matcher.match(route1));
    assertFalse(matcher.match(route2));
  }

  @Test
  void testLongNameCaseInsensitivePrefixMatch() {
    FindRoutesRequest request = FindRoutesRequest.of().withLongName("route1").build();

    Matcher<Route> matcher = RouteMatcherFactory.of(request, r -> false);
    assertTrue(matcher.match(route1));
    assertFalse(matcher.match(route2));
  }

  @Test
  void testAll() {
    FindRoutesRequest request = FindRoutesRequest
      .of()
      .withAgencies(FilterValues.ofEmptyIsEverything("agencies", List.of("agencyId")))
      .withTransitModes(FilterValues.ofEmptyIsEverything("transitModes", List.of(TransitMode.BUS)))
      .withShortName("ROUTE1")
      .withShortNames(FilterValues.ofEmptyIsEverything("publicCodes", List.of("ROUTE1", "ROUTE3")))
      .withFlexibleOnly(true)
      .withLongName("ROUTE1")
      .build();

    Set<Route> flexRoutes = Set.of(route1);

    Matcher<Route> matcher = RouteMatcherFactory.of(request, flexRoutes::contains);
    assertTrue(matcher.match(route1));
    assertFalse(matcher.match(route2));
  }
}
