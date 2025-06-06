package org.opentripplanner.apis.gtfs.mapping.routerequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.RouteRequest;

class RouteRequestMapperFiltersTest {

  private final _RouteRequestTestContext testCtx = _RouteRequestTestContext.of(Locale.GERMAN);

  private static final Map<String, Object> TRAM_AND_FERRY_MODES = Map.of(
    "transit",
    Map.of("transit", List.of(Map.of("mode", "TRAM"), Map.of("mode", "FERRY")))
  );
  public static final Map<String, Object> INCLUDE_ROUTE = Map.of(
    "include",
    List.of(Map.of("routes", List.of("f:r1")))
  );
  public static final Map<String, Object> EXCLUDE_AGENCY = Map.of(
    "exclude",
    List.of(Map.of("agencies", List.of("f:a1")))
  );
  private static final Map<String, Object> INCLUDE_ROUTE_FILTERS = Map.of(
    "transit",
    Map.of("filters", List.of(INCLUDE_ROUTE))
  );
  private static final Map<String, Object> INCLUDE_ROUTE_EXCLUDE_AGENCY_FILTERS = Map.of(
    "transit",
    Map.of("filters", List.of(INCLUDE_ROUTE, EXCLUDE_AGENCY))
  );

  @Test
  void modesAndFilter() {
    var args = testCtx.basicRequest();
    args.put("modes", TRAM_AND_FERRY_MODES);
    args.put("preferences", INCLUDE_ROUTE_FILTERS);
    assertTransitFilters(
      "[TransitFilterRequest{select: [SelectRequest{transportModes: [FERRY, TRAM], routes: [f:r1]}]}]",
      args
    );
  }

  @Test
  void modesOnly() {
    var args = testCtx.basicRequest();
    args.put("modes", TRAM_AND_FERRY_MODES);
    assertTransitFilters(
      "[TransitFilterRequest{select: [SelectRequest{transportModes: [FERRY, TRAM]}]}]",
      args
    );
  }

  @Test
  void filtersOnly() {
    var args = testCtx.basicRequest();
    args.put("preferences", INCLUDE_ROUTE_FILTERS);
    assertTransitFilters(
      "[TransitFilterRequest{select: [SelectRequest{transportModes: ALL-MAIN-MODES, routes: [f:r1]}]}]",
      args
    );
  }

  @Test
  void twoFilters() {
    var args = testCtx.basicRequest();
    args.put("preferences", INCLUDE_ROUTE_EXCLUDE_AGENCY_FILTERS);
    assertTransitFilters(
      "[TransitFilterRequest{select: [SelectRequest{transportModes: ALL-MAIN-MODES, routes: [f:r1]}]}, TransitFilterRequest{select: [SelectRequest{transportModes: ALL-MAIN-MODES}], not: [SelectRequest{transportModes: [], agencies: [f:a1]}]}]",
      args
    );
  }

  @Test
  void modesAndtwoFilters() {
    var args = testCtx.basicRequest();
    args.put("modes", TRAM_AND_FERRY_MODES);
    args.put("preferences", INCLUDE_ROUTE_EXCLUDE_AGENCY_FILTERS);
    assertTransitFilters(
      "[TransitFilterRequest{select: [SelectRequest{transportModes: [FERRY, TRAM], routes: [f:r1]}]}, TransitFilterRequest{select: [SelectRequest{transportModes: [FERRY, TRAM]}], not: [SelectRequest{transportModes: [], agencies: [f:a1]}]}]",
      args
    );
  }

  private void assertTransitFilters(String expected, Map<String, Object> modesArgs) {
    var env = testCtx.executionContext(modesArgs);

    //var env = executionContext(modesArgs, Locale.ENGLISH, RouteRequestMapperTest.CONTEXT);

    var routeRequest = RouteRequestMapper.toRouteRequest(env, testCtx.context());
    var filtersAsString = routeRequest.journey().transit().filters().toString();
    assertEquals(expected, filtersAsString);
  }
}
