package org.opentripplanner.apis.gtfs.mapping.routerequest;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.model.Cost;

class RouteRequestMapperWalkTest {

  private final _RouteRequestTestContext testCtx = _RouteRequestTestContext.of(Locale.ENGLISH);

  @Test
  void testWalkPreferences() {
    var walkArgs = testCtx.basicRequest();
    var reluctance = 7.5;
    var speed = 15d;
    var boardCost = Cost.costOfSeconds(50);
    var safetyFactor = 0.4;
    walkArgs.put(
      "preferences",
      Map.ofEntries(
        entry(
          "street",
          Map.ofEntries(
            entry(
              "walk",
              Map.ofEntries(
                entry("reluctance", reluctance),
                entry("speed", speed),
                entry("boardCost", boardCost),
                entry("safetyFactor", safetyFactor)
              )
            )
          )
        )
      )
    );
    var env = testCtx.executionContext(walkArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, testCtx.context());
    var walkPreferences = routeRequest.preferences().walk();
    assertEquals(reluctance, walkPreferences.reluctance());
    assertEquals(speed, walkPreferences.speed());
    assertEquals(boardCost.toSeconds(), walkPreferences.boardCost());
    assertEquals(safetyFactor, walkPreferences.safetyFactor());
  }
}
