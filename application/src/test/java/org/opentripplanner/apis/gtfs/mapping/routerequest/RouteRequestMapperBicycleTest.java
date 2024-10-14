package org.opentripplanner.apis.gtfs.mapping.routerequest;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.apis.gtfs.mapping.routerequest.RouteRequestMapperTest.createArgsCopy;
import static org.opentripplanner.apis.gtfs.mapping.routerequest.RouteRequestMapperTest.executionContext;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.routing.core.VehicleRoutingOptimizeType;

class RouteRequestMapperBicycleTest {

  @Test
  void testBasicBikePreferences() {
    var bicycleArgs = createArgsCopy(RouteRequestMapperTest.ARGS);
    var reluctance = 7.5;
    var speed = 15d;
    var boardCost = Cost.costOfSeconds(50);
    bicycleArgs.put(
      "preferences",
      Map.ofEntries(
        entry(
          "street",
          Map.ofEntries(
            entry(
              "bicycle",
              Map.ofEntries(
                entry("reluctance", reluctance),
                entry("speed", speed),
                entry("boardCost", boardCost)
              )
            )
          )
        )
      )
    );
    var env = executionContext(bicycleArgs, Locale.ENGLISH, RouteRequestMapperTest.CONTEXT);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, RouteRequestMapperTest.CONTEXT);
    var bikePreferences = routeRequest.preferences().bike();
    assertEquals(reluctance, bikePreferences.reluctance());
    assertEquals(speed, bikePreferences.speed());
    assertEquals(boardCost.toSeconds(), bikePreferences.boardCost());
  }

  @Test
  void testBikeWalkPreferences() {
    var bicycleArgs = createArgsCopy(RouteRequestMapperTest.ARGS);
    var walkSpeed = 7d;
    var mountDismountTime = Duration.ofSeconds(23);
    var mountDismountCost = Cost.costOfSeconds(35);
    var walkReluctance = 6.3;
    bicycleArgs.put(
      "preferences",
      Map.ofEntries(
        entry(
          "street",
          Map.ofEntries(
            entry(
              "bicycle",
              Map.ofEntries(
                entry(
                  "walk",
                  Map.ofEntries(
                    entry("speed", walkSpeed),
                    entry("mountDismountTime", mountDismountTime),
                    entry(
                      "cost",
                      Map.ofEntries(
                        entry("mountDismountCost", mountDismountCost),
                        entry("reluctance", walkReluctance)
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
    var env = executionContext(bicycleArgs, Locale.ENGLISH, RouteRequestMapperTest.CONTEXT);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, RouteRequestMapperTest.CONTEXT);
    var bikeWalkingPreferences = routeRequest.preferences().bike().walking();
    assertEquals(walkSpeed, bikeWalkingPreferences.speed());
    assertEquals(mountDismountTime, bikeWalkingPreferences.mountDismountTime());
    assertEquals(mountDismountCost, bikeWalkingPreferences.mountDismountCost());
    assertEquals(walkReluctance, bikeWalkingPreferences.reluctance());
  }

  @Test
  void testBikeTrianglePreferences() {
    var bicycleArgs = createArgsCopy(RouteRequestMapperTest.ARGS);
    var bikeSafety = 0.3;
    var bikeFlatness = 0.5;
    var bikeTime = 0.2;
    bicycleArgs.put(
      "preferences",
      Map.ofEntries(
        entry(
          "street",
          Map.ofEntries(
            entry(
              "bicycle",
              Map.ofEntries(
                entry(
                  "optimization",
                  Map.ofEntries(
                    entry(
                      "triangle",
                      Map.ofEntries(
                        entry("safety", bikeSafety),
                        entry("flatness", bikeFlatness),
                        entry("time", bikeTime)
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
    var env = executionContext(bicycleArgs, Locale.ENGLISH, RouteRequestMapperTest.CONTEXT);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, RouteRequestMapperTest.CONTEXT);
    var bikePreferences = routeRequest.preferences().bike();
    assertEquals(VehicleRoutingOptimizeType.TRIANGLE, bikePreferences.optimizeType());
    var bikeTrianglePreferences = bikePreferences.optimizeTriangle();
    assertEquals(bikeSafety, bikeTrianglePreferences.safety());
    assertEquals(bikeFlatness, bikeTrianglePreferences.slope());
    assertEquals(bikeTime, bikeTrianglePreferences.time());
  }

  @Test
  void testBikeOptimizationPreferences() {
    var bicycleArgs = createArgsCopy(RouteRequestMapperTest.ARGS);
    bicycleArgs.put(
      "preferences",
      Map.ofEntries(
        entry(
          "street",
          Map.ofEntries(
            entry(
              "bicycle",
              Map.ofEntries(entry("optimization", Map.ofEntries(entry("type", "SAFEST_STREETS"))))
            )
          )
        )
      )
    );
    var env = executionContext(bicycleArgs, Locale.ENGLISH, RouteRequestMapperTest.CONTEXT);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, RouteRequestMapperTest.CONTEXT);
    var bikePreferences = routeRequest.preferences().bike();
    assertEquals(VehicleRoutingOptimizeType.SAFEST_STREETS, bikePreferences.optimizeType());
  }

  @Test
  void testBikeRentalPreferences() {
    var bicycleArgs = createArgsCopy(RouteRequestMapperTest.ARGS);
    var allowed = Set.of("foo", "bar");
    var banned = Set.of("not");
    var allowKeeping = true;
    var keepingCost = Cost.costOfSeconds(150);
    bicycleArgs.put(
      "preferences",
      Map.ofEntries(
        entry(
          "street",
          Map.ofEntries(
            entry(
              "bicycle",
              Map.ofEntries(
                entry(
                  "rental",
                  Map.ofEntries(
                    entry("allowedNetworks", allowed.stream().toList()),
                    entry("bannedNetworks", banned.stream().toList()),
                    entry(
                      "destinationBicyclePolicy",
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
    var env = executionContext(bicycleArgs, Locale.ENGLISH, RouteRequestMapperTest.CONTEXT);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, RouteRequestMapperTest.CONTEXT);
    var bikeRentalPreferences = routeRequest.preferences().bike().rental();
    assertEquals(allowed, bikeRentalPreferences.allowedNetworks());
    assertEquals(banned, bikeRentalPreferences.bannedNetworks());
    assertEquals(allowKeeping, bikeRentalPreferences.allowArrivingInRentedVehicleAtDestination());
    assertEquals(keepingCost, bikeRentalPreferences.arrivingInRentalVehicleAtDestinationCost());
  }

  @Test
  void testEmptyBikeRentalPreferences() {
    var bikeArgs = createArgsCopy(RouteRequestMapperTest.ARGS);
    var empty = Set.of();
    bikeArgs.put(
      "preferences",
      Map.ofEntries(
        entry(
          "street",
          Map.ofEntries(
            entry(
              "bicycle",
              Map.ofEntries(
                entry("rental", Map.ofEntries(entry("allowedNetworks", empty.stream().toList())))
              )
            )
          )
        )
      )
    );
    var allowedEnv = executionContext(bikeArgs, Locale.ENGLISH, RouteRequestMapperTest.CONTEXT);
    assertThrows(
      IllegalArgumentException.class,
      () -> RouteRequestMapper.toRouteRequest(allowedEnv, RouteRequestMapperTest.CONTEXT)
    );

    bikeArgs = createArgsCopy(RouteRequestMapperTest.ARGS);
    bikeArgs.put(
      "preferences",
      Map.ofEntries(
        entry(
          "street",
          Map.ofEntries(
            entry(
              "bicycle",
              Map.ofEntries(
                entry("rental", Map.ofEntries(entry("bannedNetworks", empty.stream().toList())))
              )
            )
          )
        )
      )
    );
    var bannedEnv = executionContext(bikeArgs, Locale.ENGLISH, RouteRequestMapperTest.CONTEXT);
    var routeRequest = RouteRequestMapper.toRouteRequest(bannedEnv, RouteRequestMapperTest.CONTEXT);
    var bikeRentalPreferences = routeRequest.preferences().bike().rental();
    assertEquals(empty, bikeRentalPreferences.bannedNetworks());
  }

  @Test
  void testBikeParkingPreferences() {
    var bicycleArgs = createArgsCopy(RouteRequestMapperTest.ARGS);
    var unpreferredCost = Cost.costOfSeconds(150);
    var notFilter = List.of("wheelbender");
    var selectFilter = List.of("locker", "roof");
    var unpreferred = List.of("bad");
    var preferred = List.of("a", "b");
    bicycleArgs.put(
      "preferences",
      Map.ofEntries(
        entry(
          "street",
          Map.ofEntries(
            entry(
              "bicycle",
              Map.ofEntries(
                entry(
                  "parking",
                  Map.ofEntries(
                    entry("unpreferredCost", unpreferredCost),
                    entry(
                      "filters",
                      List.of(
                        Map.ofEntries(
                          entry("not", List.of(Map.of("tags", notFilter))),
                          entry("select", List.of(Map.of("tags", selectFilter)))
                        )
                      )
                    ),
                    entry(
                      "preferred",
                      List.of(
                        Map.ofEntries(
                          entry("not", List.of(Map.of("tags", unpreferred))),
                          entry("select", List.of(Map.of("tags", preferred)))
                        )
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
    var env = executionContext(bicycleArgs, Locale.ENGLISH, RouteRequestMapperTest.CONTEXT);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, RouteRequestMapperTest.CONTEXT);
    var bikeParkingPreferences = routeRequest.preferences().bike().parking();
    assertEquals(unpreferredCost, bikeParkingPreferences.unpreferredVehicleParkingTagCost());
    assertEquals(
      "VehicleParkingFilter{not: [tags=%s], select: [tags=%s]}".formatted(notFilter, selectFilter),
      bikeParkingPreferences.filter().toString()
    );
    assertEquals(
      "VehicleParkingFilter{not: [tags=%s], select: [tags=%s]}".formatted(unpreferred, preferred),
      bikeParkingPreferences.preferred().toString()
    );
  }
}
