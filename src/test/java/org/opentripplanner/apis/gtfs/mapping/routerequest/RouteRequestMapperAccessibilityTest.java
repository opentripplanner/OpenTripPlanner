package org.opentripplanner.apis.gtfs.mapping.routerequest;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.apis.gtfs.mapping.routerequest.RouteRequestMapperTest.createArgsCopy;
import static org.opentripplanner.apis.gtfs.mapping.routerequest.RouteRequestMapperTest.executionContext;

import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RouteRequestMapperAccessibilityTest {

  @Test
  void testWheelchairPreferences() {
    var args = createArgsCopy(RouteRequestMapperTest.ARGS);
    var wheelchairEnabled = true;
    args.put(
      "preferences",
      Map.ofEntries(
        entry(
          "accessibility",
          Map.ofEntries(entry("wheelchair", Map.ofEntries(entry("enabled", wheelchairEnabled))))
        )
      )
    );
    var env = executionContext(args, Locale.ENGLISH, RouteRequestMapperTest.CONTEXT);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, RouteRequestMapperTest.CONTEXT);
    assertEquals(wheelchairEnabled, routeRequest.wheelchair());
  }
}
