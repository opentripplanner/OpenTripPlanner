package org.opentripplanner.apis.gtfs.mapping.routerequest;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.apis.gtfs.mapping.routerequest.RouteRequestMapperTest.createArgsCopy;
import static org.opentripplanner.apis.gtfs.mapping.routerequest.RouteRequestMapperTest.executionContext;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RouteRequestMapperFiltersTest {

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
    var args = createArgsCopy(RouteRequestMapperTest.ARGS);
    args.put("modes", TRAM_AND_FERRY_MODES);
    args.put("preferences", INCLUDE_ROUTE_FILTERS);
    assertTransitFilters(
      "[TransitFilterRequest{select: [SelectRequest{transportModes: [FERRY, TRAM]}, SelectRequest{transportModes: [], routes: [f:r1]}]}]",
      args
    );
  }

  @Test
  void modesOnly() {
    var args = createArgsCopy(RouteRequestMapperTest.ARGS);
    args.put("modes", TRAM_AND_FERRY_MODES);
    assertTransitFilters(
      "[TransitFilterRequest{select: [SelectRequest{transportModes: [FERRY, TRAM]}]}]",
      args
    );
  }

  @Test
  void filtersOnly() {
    var args = createArgsCopy(RouteRequestMapperTest.ARGS);
    args.put("preferences", INCLUDE_ROUTE_FILTERS);
    assertTransitFilters(
      "[TransitFilterRequest{select: [SelectRequest{transportModes: ALL-MAIN-MODES}, SelectRequest{transportModes: [], routes: [f:r1]}]}]",
      args
    );
  }

  @Test
  void twoFilters() {
    var args = createArgsCopy(RouteRequestMapperTest.ARGS);
    args.put("preferences", INCLUDE_ROUTE_EXCLUDE_AGENCY_FILTERS);
    assertTransitFilters(
      "[TransitFilterRequest{select: [SelectRequest{transportModes: ALL-MAIN-MODES}, SelectRequest{transportModes: [], routes: [f:r1]}]}, TransitFilterRequest{select: [SelectRequest{transportModes: ALL-MAIN-MODES}], not: [SelectRequest{transportModes: [], agencies: [f:a1]}]}]",
      args
    );
  }

  @Test
  void modesAndtwoFilters() {
    var args = createArgsCopy(RouteRequestMapperTest.ARGS);
    args.put("modes", TRAM_AND_FERRY_MODES);
    args.put("preferences", INCLUDE_ROUTE_EXCLUDE_AGENCY_FILTERS);
    assertTransitFilters(
      "[TransitFilterRequest{select: [SelectRequest{transportModes: [FERRY, TRAM]}, SelectRequest{transportModes: [], routes: [f:r1]}]}, TransitFilterRequest{select: [SelectRequest{transportModes: [FERRY, TRAM]}], not: [SelectRequest{transportModes: [], agencies: [f:a1]}]}]",
      args
    );
  }

  private static void assertTransitFilters(String expected, Map<String, Object> modesArgs) {
    var env = executionContext(modesArgs, Locale.ENGLISH, RouteRequestMapperTest.CONTEXT);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, RouteRequestMapperTest.CONTEXT);
    var filtersAsString = routeRequest.journey().transit().filters().toString();
    assertEquals(expected, filtersAsString);
  }
}
