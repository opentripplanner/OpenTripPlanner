package org.opentripplanner.apis.gtfs.mapping.routerequest;

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
import org.opentripplanner.routing.api.request.RouteRequest;

class RouteRequestMapperFiltersTest {

  private static final Map<String, Object> TRAM_AND_FERRY_MODES = Map.of(
    "transit",
    Map.of("transit", List.of(Map.of("mode", "TRAM"), Map.of("mode", "FERRY")))
  );
  private static final Map<String, Object> TRANSIT = Map.of(
    "transit",
    Map.of("filters", List.of(Map.of("include", List.of(Map.of("routes", List.of("f:r1"))))))
  );

  @Test
  void modesAndFilter() {
    var args = createArgsCopy(RouteRequestMapperTest.ARGS);
    args.put("modes", TRAM_AND_FERRY_MODES);
    args.put("preferences", TRANSIT);
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

  private static void assertTransitFilters(String expected, Map<String, Object> modesArgs) {
    var env = executionContext(modesArgs, Locale.ENGLISH, RouteRequestMapperTest.CONTEXT);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, RouteRequestMapperTest.CONTEXT);
    var filtersAsString = routeRequest.journey().transit().filters().toString();
    assertEquals(expected, filtersAsString);
  }
}
