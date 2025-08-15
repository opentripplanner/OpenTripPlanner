package org.opentripplanner.apis.gtfs.mapping.routerequest;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.transit.model.basic.TransitMode;

class RouteRequestMapperModesTest {

  private final _RouteRequestTestContext testCtx = _RouteRequestTestContext.of(Locale.ENGLISH);

  @Test
  void testDirectOnly() {
    var modesArgs = testCtx.basicRequest();
    modesArgs.put("modes", Map.ofEntries(entry("directOnly", true)));
    var env = testCtx.executionContext(modesArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, testCtx.context());
    assertFalse(routeRequest.journey().transit().enabled());
  }

  @Test
  void testTransitOnly() {
    var modesArgs = testCtx.basicRequest();
    modesArgs.put("modes", Map.ofEntries(entry("transitOnly", true)));
    var env = testCtx.executionContext(modesArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, testCtx.context());
    assertEquals(StreetMode.NOT_SET, routeRequest.journey().direct().mode());
  }

  @Test
  void testStreetModesWithOneValidMode() {
    var modesArgs = testCtx.basicRequest();
    var bicycle = List.of("BICYCLE");
    modesArgs.put(
      "modes",
      Map.ofEntries(
        entry("direct", List.of("CAR")),
        entry(
          "transit",
          Map.ofEntries(
            entry("access", bicycle),
            entry("egress", bicycle),
            entry("transfer", bicycle)
          )
        )
      )
    );
    var env = testCtx.executionContext(modesArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, testCtx.context());
    assertEquals(StreetMode.CAR, routeRequest.journey().direct().mode());
    assertEquals(StreetMode.BIKE, routeRequest.journey().access().mode());
    assertEquals(StreetMode.BIKE, routeRequest.journey().egress().mode());
    assertEquals(StreetMode.BIKE, routeRequest.journey().transfer().mode());
  }

  @Test
  void testStreetModesWithOneInvalidMode() {
    var modesArgs = testCtx.basicRequest();
    var bicycleRental = List.of("BICYCLE_RENTAL");
    modesArgs.put("modes", Map.ofEntries(entry("direct", bicycleRental)));
    var env = testCtx.executionContext(modesArgs);
    assertThrows(IllegalArgumentException.class, () ->
      RouteRequestMapper.toRouteRequest(env, testCtx.context())
    );
  }

  @Test
  void testStreetModesWithTwoValidModes() {
    var modesArgs = testCtx.basicRequest();
    var bicycleRental = List.of("BICYCLE_RENTAL", "WALK");
    modesArgs.put(
      "modes",
      Map.ofEntries(
        entry("direct", bicycleRental),
        entry(
          "transit",
          Map.ofEntries(entry("access", bicycleRental), entry("egress", bicycleRental))
        )
      )
    );
    var env = testCtx.executionContext(modesArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, testCtx.context());
    assertEquals(StreetMode.BIKE_RENTAL, routeRequest.journey().direct().mode());
    assertEquals(StreetMode.BIKE_RENTAL, routeRequest.journey().access().mode());
    assertEquals(StreetMode.BIKE_RENTAL, routeRequest.journey().egress().mode());
    assertEquals(StreetMode.WALK, routeRequest.journey().transfer().mode());
  }

  @Test
  void testStreetModesWithTwoInvalidModes() {
    var modesArgs = testCtx.basicRequest();
    var rentals = List.of("BICYCLE_RENTAL", "CAR_RENTAL");
    modesArgs.put(
      "modes",
      Map.ofEntries(
        entry("direct", rentals),
        entry("transit", Map.ofEntries(entry("access", rentals), entry("egress", rentals)))
      )
    );
    var rentalEnv = testCtx.executionContext(modesArgs);
    assertThrows(IllegalArgumentException.class, () ->
      RouteRequestMapper.toRouteRequest(rentalEnv, testCtx.context())
    );

    modesArgs = testCtx.basicRequest();
    var bicycleWalk = List.of("BICYCLE", "WALK");
    modesArgs.put(
      "modes",
      Map.ofEntries(
        entry("transit", Map.ofEntries(entry("access", bicycleWalk), entry("egress", bicycleWalk)))
      )
    );
    var bicycleWalkEnv = testCtx.executionContext(modesArgs);
    assertThrows(IllegalArgumentException.class, () ->
      RouteRequestMapper.toRouteRequest(bicycleWalkEnv, testCtx.context())
    );
  }

  @Test
  void testStreetModesWithThreeModes() {
    var modesArgs = testCtx.basicRequest();
    var rentals = List.of("WALK", "BICYCLE_RENTAL", "CAR_RENTAL");
    modesArgs.put(
      "modes",
      Map.ofEntries(
        entry("direct", rentals),
        entry("transit", Map.ofEntries(entry("access", rentals), entry("egress", rentals)))
      )
    );
    var env = testCtx.executionContext(modesArgs);
    assertThrows(IllegalArgumentException.class, () ->
      RouteRequestMapper.toRouteRequest(env, testCtx.context())
    );
  }

  @Test
  void testTransitModes() {
    var modesArgs = testCtx.basicRequest();
    var tramCost = 1.5;
    modesArgs.put(
      "modes",
      Map.ofEntries(
        entry(
          "transit",
          Map.ofEntries(
            entry(
              "transit",
              List.of(
                Map.ofEntries(
                  entry("mode", "TRAM"),
                  entry("cost", Map.ofEntries(entry("reluctance", tramCost)))
                ),
                Map.ofEntries(entry("mode", "FERRY"))
              )
            )
          )
        )
      )
    );
    var env = testCtx.executionContext(modesArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, testCtx.context());
    var reluctanceForMode = routeRequest.preferences().transit().reluctanceForMode();
    assertEquals(tramCost, reluctanceForMode.get(TransitMode.TRAM));
    assertNull(reluctanceForMode.get(TransitMode.FERRY));
    assertEquals(
      "[(select: [(transportModes: [FERRY, TRAM])])]",
      routeRequest.journey().transit().filters().toString()
    );
  }

  @Test
  void testStreetModesWithEmptyModes() {
    var modesArgs = testCtx.basicRequest();
    var empty = List.of();
    modesArgs.put("modes", Map.ofEntries(entry("direct", empty)));
    var directEnv = testCtx.executionContext(modesArgs);
    assertThrows(IllegalArgumentException.class, () ->
      RouteRequestMapper.toRouteRequest(directEnv, testCtx.context())
    );

    modesArgs = testCtx.basicRequest();
    modesArgs.put("modes", Map.ofEntries(entry("transit", Map.ofEntries(entry("access", empty)))));
    var accessEnv = testCtx.executionContext(modesArgs);
    assertThrows(IllegalArgumentException.class, () ->
      RouteRequestMapper.toRouteRequest(accessEnv, testCtx.context())
    );

    modesArgs = testCtx.basicRequest();
    modesArgs.put("modes", Map.ofEntries(entry("transit", Map.ofEntries(entry("egress", empty)))));
    var egressEnv = testCtx.executionContext(modesArgs);
    assertThrows(IllegalArgumentException.class, () ->
      RouteRequestMapper.toRouteRequest(egressEnv, testCtx.context())
    );

    modesArgs = testCtx.basicRequest();
    modesArgs.put(
      "modes",
      Map.ofEntries(entry("transit", Map.ofEntries(entry("transfer", empty))))
    );
    var transferEnv = testCtx.executionContext(modesArgs);
    assertThrows(IllegalArgumentException.class, () ->
      RouteRequestMapper.toRouteRequest(transferEnv, testCtx.context())
    );

    modesArgs = testCtx.basicRequest();
    modesArgs.put("modes", Map.ofEntries(entry("transit", Map.ofEntries(entry("transit", empty)))));
    var transitEnv = testCtx.executionContext(modesArgs);
    assertThrows(IllegalArgumentException.class, () ->
      RouteRequestMapper.toRouteRequest(transitEnv, testCtx.context())
    );
  }
}
