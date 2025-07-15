package org.opentripplanner.apis.gtfs.mapping.routerequest;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RouteRequestMapperAccessibilityTest {

  private final _RouteRequestTestContext testCtx = _RouteRequestTestContext.of(Locale.ENGLISH);

  @Test
  void testWheelchairPreferences() {
    var args = testCtx.basicRequest();
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
    var env = testCtx.executionContext(args);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, testCtx.context());
    assertEquals(wheelchairEnabled, routeRequest.journey().wheelchair());
  }
}
