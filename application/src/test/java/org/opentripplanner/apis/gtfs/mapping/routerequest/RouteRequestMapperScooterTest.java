package org.opentripplanner.apis.gtfs.mapping.routerequest;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.routing.core.VehicleRoutingOptimizeType;

class RouteRequestMapperScooterTest {

  private final _RouteRequestTestContext testCtx = _RouteRequestTestContext.of(Locale.ENGLISH);

  @Test
  void testBasicScooterPreferences() {
    var scooterArgs = testCtx.basicRequest();
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
    var env = testCtx.executionContext(scooterArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, testCtx.context());
    var scooterPreferences = routeRequest.preferences().scooter();
    assertEquals(reluctance, scooterPreferences.reluctance());
    assertEquals(speed, scooterPreferences.speed());
  }

  @Test
  void testScooterTrianglePreferences() {
    var scooterArgs = testCtx.basicRequest();
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
    var env = testCtx.executionContext(scooterArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, testCtx.context());
    var scooterPreferences = routeRequest.preferences().scooter();
    assertEquals(VehicleRoutingOptimizeType.TRIANGLE, scooterPreferences.optimizeType());
    var scooterTrianglePreferences = scooterPreferences.optimizeTriangle();
    assertEquals(scooterSafety, scooterTrianglePreferences.safety());
    assertEquals(scooterFlatness, scooterTrianglePreferences.slope());
    assertEquals(scooterTime, scooterTrianglePreferences.time());
  }

  @Test
  void testScooterOptimizationPreferences() {
    var scooterArgs = testCtx.basicRequest();
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
    var env = testCtx.executionContext(scooterArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, testCtx.context());
    var scooterPreferences = routeRequest.preferences().scooter();
    assertEquals(VehicleRoutingOptimizeType.SAFEST_STREETS, scooterPreferences.optimizeType());
  }

  @Test
  void testScooterRentalPreferences() {
    var scooterArgs = testCtx.basicRequest();
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
    var env = testCtx.executionContext(scooterArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, testCtx.context());
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
    var scooterArgs = testCtx.basicRequest();
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
    var allowedEnv = testCtx.executionContext(scooterArgs);
    assertThrows(IllegalArgumentException.class, () ->
      RouteRequestMapper.toRouteRequest(allowedEnv, testCtx.context())
    );

    scooterArgs = testCtx.basicRequest();
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
    var bannedEnv = testCtx.executionContext(scooterArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(bannedEnv, testCtx.context());
    var scooterRentalPreferences = routeRequest.preferences().scooter().rental();
    assertEquals(empty, scooterRentalPreferences.bannedNetworks());
  }
}
