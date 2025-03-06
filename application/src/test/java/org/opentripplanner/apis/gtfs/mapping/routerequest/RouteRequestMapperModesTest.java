package org.opentripplanner.apis.gtfs.mapping.routerequest;

import static java.util.Map.entry;
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
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.transit.model.basic.TransitMode;

class RouteRequestMapperModesTest {

  @Test
  void testDirectOnly() {
    var modesArgs = createArgsCopy(RouteRequestMapperTest.ARGS);
    modesArgs.put("modes", Map.ofEntries(entry("directOnly", true)));
    var env = executionContext(modesArgs, Locale.ENGLISH, RouteRequestMapperTest.CONTEXT);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, RouteRequestMapperTest.CONTEXT);
    assertFalse(routeRequest.journey().transit().enabled());
  }

  @Test
  void testTransitOnly() {
    var modesArgs = createArgsCopy(RouteRequestMapperTest.ARGS);
    modesArgs.put("modes", Map.ofEntries(entry("transitOnly", true)));
    var env = executionContext(modesArgs, Locale.ENGLISH, RouteRequestMapperTest.CONTEXT);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, RouteRequestMapperTest.CONTEXT);
    assertEquals(StreetMode.NOT_SET, routeRequest.journey().direct().mode());
  }

  @Test
  void testStreetModesWithOneValidMode() {
    var modesArgs = createArgsCopy(RouteRequestMapperTest.ARGS);
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
    var env = executionContext(modesArgs, Locale.ENGLISH, RouteRequestMapperTest.CONTEXT);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, RouteRequestMapperTest.CONTEXT);
    assertEquals(StreetMode.CAR, routeRequest.journey().direct().mode());
    assertEquals(StreetMode.BIKE, routeRequest.journey().access().mode());
    assertEquals(StreetMode.BIKE, routeRequest.journey().egress().mode());
    assertEquals(StreetMode.BIKE, routeRequest.journey().transfer().mode());
  }

  @Test
  void testStreetModesWithOneInvalidMode() {
    var modesArgs = createArgsCopy(RouteRequestMapperTest.ARGS);
    var bicycleRental = List.of("BICYCLE_RENTAL");
    modesArgs.put("modes", Map.ofEntries(entry("direct", bicycleRental)));
    var env = executionContext(modesArgs, Locale.ENGLISH, RouteRequestMapperTest.CONTEXT);
    assertThrows(IllegalArgumentException.class, () ->
      RouteRequestMapper.toRouteRequest(env, RouteRequestMapperTest.CONTEXT)
    );
  }

  @Test
  void testStreetModesWithTwoValidModes() {
    var modesArgs = createArgsCopy(RouteRequestMapperTest.ARGS);
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
    var env = executionContext(modesArgs, Locale.ENGLISH, RouteRequestMapperTest.CONTEXT);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, RouteRequestMapperTest.CONTEXT);
    assertEquals(StreetMode.BIKE_RENTAL, routeRequest.journey().direct().mode());
    assertEquals(StreetMode.BIKE_RENTAL, routeRequest.journey().access().mode());
    assertEquals(StreetMode.BIKE_RENTAL, routeRequest.journey().egress().mode());
    assertEquals(StreetMode.WALK, routeRequest.journey().transfer().mode());
  }

  @Test
  void testStreetModesWithTwoInvalidModes() {
    var modesArgs = createArgsCopy(RouteRequestMapperTest.ARGS);
    var rentals = List.of("BICYCLE_RENTAL", "CAR_RENTAL");
    modesArgs.put(
      "modes",
      Map.ofEntries(
        entry("direct", rentals),
        entry("transit", Map.ofEntries(entry("access", rentals), entry("egress", rentals)))
      )
    );
    var rentalEnv = executionContext(modesArgs, Locale.ENGLISH, RouteRequestMapperTest.CONTEXT);
    assertThrows(IllegalArgumentException.class, () ->
      RouteRequestMapper.toRouteRequest(rentalEnv, RouteRequestMapperTest.CONTEXT)
    );

    modesArgs = createArgsCopy(RouteRequestMapperTest.ARGS);
    var bicycleWalk = List.of("BICYCLE", "WALK");
    modesArgs.put(
      "modes",
      Map.ofEntries(
        entry("transit", Map.ofEntries(entry("access", bicycleWalk), entry("egress", bicycleWalk)))
      )
    );
    var bicycleWalkEnv = executionContext(
      modesArgs,
      Locale.ENGLISH,
      RouteRequestMapperTest.CONTEXT
    );
    assertThrows(IllegalArgumentException.class, () ->
      RouteRequestMapper.toRouteRequest(bicycleWalkEnv, RouteRequestMapperTest.CONTEXT)
    );
  }

  @Test
  void testStreetModesWithThreeModes() {
    var modesArgs = createArgsCopy(RouteRequestMapperTest.ARGS);
    var rentals = List.of("WALK", "BICYCLE_RENTAL", "CAR_RENTAL");
    modesArgs.put(
      "modes",
      Map.ofEntries(
        entry("direct", rentals),
        entry("transit", Map.ofEntries(entry("access", rentals), entry("egress", rentals)))
      )
    );
    var env = executionContext(modesArgs, Locale.ENGLISH, RouteRequestMapperTest.CONTEXT);
    assertThrows(IllegalArgumentException.class, () ->
      RouteRequestMapper.toRouteRequest(env, RouteRequestMapperTest.CONTEXT)
    );
  }

  @Test
  void testTransitModes() {
    var modesArgs = createArgsCopy(RouteRequestMapperTest.ARGS);
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
    var env = executionContext(modesArgs, Locale.ENGLISH, RouteRequestMapperTest.CONTEXT);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, RouteRequestMapperTest.CONTEXT);
    var reluctanceForMode = routeRequest.preferences().transit().reluctanceForMode();
    assertEquals(tramCost, reluctanceForMode.get(TransitMode.TRAM));
    assertNull(reluctanceForMode.get(TransitMode.FERRY));
    assertEquals(
      "[TransitFilterRequest{select: [SelectRequest{transportModes: [FERRY, TRAM]}]}]",
      routeRequest.journey().transit().filters().toString()
    );
  }

  @Test
  void testStreetModesWithEmptyModes() {
    var modesArgs = createArgsCopy(RouteRequestMapperTest.ARGS);
    var empty = List.of();
    modesArgs.put("modes", Map.ofEntries(entry("direct", empty)));
    var directEnv = executionContext(modesArgs, Locale.ENGLISH, RouteRequestMapperTest.CONTEXT);
    assertThrows(IllegalArgumentException.class, () ->
      RouteRequestMapper.toRouteRequest(directEnv, RouteRequestMapperTest.CONTEXT)
    );

    modesArgs = createArgsCopy(RouteRequestMapperTest.ARGS);
    modesArgs.put("modes", Map.ofEntries(entry("transit", Map.ofEntries(entry("access", empty)))));
    var accessEnv = executionContext(modesArgs, Locale.ENGLISH, RouteRequestMapperTest.CONTEXT);
    assertThrows(IllegalArgumentException.class, () ->
      RouteRequestMapper.toRouteRequest(accessEnv, RouteRequestMapperTest.CONTEXT)
    );

    modesArgs = createArgsCopy(RouteRequestMapperTest.ARGS);
    modesArgs.put("modes", Map.ofEntries(entry("transit", Map.ofEntries(entry("egress", empty)))));
    var egressEnv = executionContext(modesArgs, Locale.ENGLISH, RouteRequestMapperTest.CONTEXT);
    assertThrows(IllegalArgumentException.class, () ->
      RouteRequestMapper.toRouteRequest(egressEnv, RouteRequestMapperTest.CONTEXT)
    );

    modesArgs = createArgsCopy(RouteRequestMapperTest.ARGS);
    modesArgs.put(
      "modes",
      Map.ofEntries(entry("transit", Map.ofEntries(entry("transfer", empty))))
    );
    var transferEnv = executionContext(modesArgs, Locale.ENGLISH, RouteRequestMapperTest.CONTEXT);
    assertThrows(IllegalArgumentException.class, () ->
      RouteRequestMapper.toRouteRequest(transferEnv, RouteRequestMapperTest.CONTEXT)
    );

    modesArgs = createArgsCopy(RouteRequestMapperTest.ARGS);
    modesArgs.put("modes", Map.ofEntries(entry("transit", Map.ofEntries(entry("transit", empty)))));
    var transitEnv = executionContext(modesArgs, Locale.ENGLISH, RouteRequestMapperTest.CONTEXT);
    assertThrows(IllegalArgumentException.class, () ->
      RouteRequestMapper.toRouteRequest(transitEnv, RouteRequestMapperTest.CONTEXT)
    );
  }
}
