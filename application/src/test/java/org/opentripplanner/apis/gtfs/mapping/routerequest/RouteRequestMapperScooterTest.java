package org.opentripplanner.apis.gtfs.mapping.routerequest;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.apis.gtfs.mapping.routerequest.RouteRequestMapperTest.createArgsCopy;
import static org.opentripplanner.apis.gtfs.mapping.routerequest.RouteRequestMapperTest.executionContext;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.routing.core.VehicleRoutingOptimizeType;

class RouteRequestMapperScooterTest {

  @Test
  void testBasicScooterPreferences() {
    var scooterArgs = createArgsCopy(RouteRequestMapperTest.ARGS);
    var reluctance = 7.5;
    var speed = 15d;
    scooterArgs.put(
      "preferences",
      Map.ofEntries(
        entry(
          "street",
          Map.ofEntries(
            entry("scooter", Map.ofEntries(entry("reluctance", reluctance), entry("speed", speed)))
          )
        )
      )
    );
    var env = executionContext(scooterArgs, Locale.ENGLISH, RouteRequestMapperTest.CONTEXT);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, RouteRequestMapperTest.CONTEXT);
    var scooterPreferences = routeRequest.preferences().scooter();
    assertEquals(reluctance, scooterPreferences.reluctance());
    assertEquals(speed, scooterPreferences.speed());
  }

  @Test
  void testScooterTrianglePreferences() {
    var scooterArgs = createArgsCopy(RouteRequestMapperTest.ARGS);
    var scooterSafety = 0.3;
    var scooterFlatness = 0.5;
    var scooterTime = 0.2;
    scooterArgs.put(
      "preferences",
      Map.ofEntries(
        entry(
          "street",
          Map.ofEntries(
            entry(
              "scooter",
              Map.ofEntries(
                entry(
                  "optimization",
                  Map.ofEntries(
                    entry(
                      "triangle",
                      Map.ofEntries(
                        entry("safety", scooterSafety),
                        entry("flatness", scooterFlatness),
                        entry("time", scooterTime)
                      )
                    )
                  )
                )
              )
            )
          )
        )
      )
    );
    var env = executionContext(scooterArgs, Locale.ENGLISH, RouteRequestMapperTest.CONTEXT);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, RouteRequestMapperTest.CONTEXT);
    var scooterPreferences = routeRequest.preferences().scooter();
    assertEquals(VehicleRoutingOptimizeType.TRIANGLE, scooterPreferences.optimizeType());
    var scooterTrianglePreferences = scooterPreferences.optimizeTriangle();
    assertEquals(scooterSafety, scooterTrianglePreferences.safety());
    assertEquals(scooterFlatness, scooterTrianglePreferences.slope());
    assertEquals(scooterTime, scooterTrianglePreferences.time());
  }

  @Test
  void testScooterOptimizationPreferences() {
    var scooterArgs = createArgsCopy(RouteRequestMapperTest.ARGS);
    scooterArgs.put(
      "preferences",
      Map.ofEntries(
        entry(
          "street",
          Map.ofEntries(
            entry(
              "scooter",
              Map.ofEntries(entry("optimization", Map.ofEntries(entry("type", "SAFEST_STREETS"))))
            )
          )
        )
      )
    );
    var env = executionContext(scooterArgs, Locale.ENGLISH, RouteRequestMapperTest.CONTEXT);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, RouteRequestMapperTest.CONTEXT);
    var scooterPreferences = routeRequest.preferences().scooter();
    assertEquals(VehicleRoutingOptimizeType.SAFEST_STREETS, scooterPreferences.optimizeType());
  }

  @Test
  void testScooterRentalPreferences() {
    var scooterArgs = createArgsCopy(RouteRequestMapperTest.ARGS);
    var allowed = Set.of("foo", "bar");
    var banned = Set.of("not");
    var allowKeeping = true;
    var keepingCost = Cost.costOfSeconds(150);
    scooterArgs.put(
      "preferences",
      Map.ofEntries(
        entry(
          "street",
          Map.ofEntries(
            entry(
              "scooter",
              Map.ofEntries(
                entry(
                  "rental",
                  Map.ofEntries(
                    entry("allowedNetworks", allowed.stream().toList()),
                    entry("bannedNetworks", banned.stream().toList()),
                    entry(
                      "destinationScooterPolicy",
                      Map.ofEntries(
                        entry("allowKeeping", allowKeeping),
                        entry("keepingCost", keepingCost)
                      )
                    )
                  )
                )
              )
            )
          )
        )
      )
    );
    var env = executionContext(scooterArgs, Locale.ENGLISH, RouteRequestMapperTest.CONTEXT);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, RouteRequestMapperTest.CONTEXT);
    var scooterRentalPreferences = routeRequest.preferences().scooter().rental();
    assertEquals(allowed, scooterRentalPreferences.allowedNetworks());
    assertEquals(banned, scooterRentalPreferences.bannedNetworks());
    assertEquals(
      allowKeeping,
      scooterRentalPreferences.allowArrivingInRentedVehicleAtDestination()
    );
    assertEquals(keepingCost, scooterRentalPreferences.arrivingInRentalVehicleAtDestinationCost());
  }

  @Test
  void testEmptyScooterRentalPreferences() {
    var scooterArgs = createArgsCopy(RouteRequestMapperTest.ARGS);
    var empty = Set.of();
    scooterArgs.put(
      "preferences",
      Map.ofEntries(
        entry(
          "street",
          Map.ofEntries(
            entry(
              "scooter",
              Map.ofEntries(
                entry("rental", Map.ofEntries(entry("allowedNetworks", empty.stream().toList())))
              )
            )
          )
        )
      )
    );
    var allowedEnv = executionContext(scooterArgs, Locale.ENGLISH, RouteRequestMapperTest.CONTEXT);
    assertThrows(IllegalArgumentException.class, () ->
      RouteRequestMapper.toRouteRequest(allowedEnv, RouteRequestMapperTest.CONTEXT)
    );

    scooterArgs = createArgsCopy(RouteRequestMapperTest.ARGS);
    scooterArgs.put(
      "preferences",
      Map.ofEntries(
        entry(
          "street",
          Map.ofEntries(
            entry(
              "scooter",
              Map.ofEntries(
                entry("rental", Map.ofEntries(entry("bannedNetworks", empty.stream().toList())))
              )
            )
          )
        )
      )
    );
    var bannedEnv = executionContext(scooterArgs, Locale.ENGLISH, RouteRequestMapperTest.CONTEXT);
    var routeRequest = RouteRequestMapper.toRouteRequest(bannedEnv, RouteRequestMapperTest.CONTEXT);
    var scooterRentalPreferences = routeRequest.preferences().scooter().rental();
    assertEquals(empty, scooterRentalPreferences.bannedNetworks());
  }
}
