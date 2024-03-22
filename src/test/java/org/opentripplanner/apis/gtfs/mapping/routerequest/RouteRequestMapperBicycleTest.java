package org.opentripplanner.apis.gtfs.mapping.routerequest;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.apis.gtfs.mapping.routerequest.RouteRequestMapperTest.createArgsCopy;
import static org.opentripplanner.apis.gtfs.mapping.routerequest.RouteRequestMapperTest.executionContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.apis.gtfs.GraphQLRequestContext;
import org.opentripplanner.apis.gtfs.TestRoutingService;
import org.opentripplanner.ext.fares.impl.DefaultFareService;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.core.VehicleRoutingOptimizeType;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graphfinder.GraphFinder;
import org.opentripplanner.service.realtimevehicles.internal.DefaultRealtimeVehicleService;
import org.opentripplanner.service.vehiclerental.internal.DefaultVehicleRentalService;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;

class RouteRequestMapperBicycleTest {

  private static final GraphQLRequestContext context;
  private static final Map<String, Object> args = new HashMap<>();

  static {
    args.put(
      "origin",
      Map.ofEntries(
        entry("location", Map.of("coordinate", Map.of("latitude", 1.0, "longitude", 2.0)))
      )
    );
    args.put(
      "destination",
      Map.ofEntries(
        entry("location", Map.of("coordinate", Map.of("latitude", 2.0, "longitude", 1.0)))
      )
    );

    Graph graph = new Graph();
    var transitModel = new TransitModel();
    transitModel.initTimeZone(ZoneIds.BERLIN);
    final DefaultTransitService transitService = new DefaultTransitService(transitModel);
    context =
      new GraphQLRequestContext(
        new TestRoutingService(List.of()),
        transitService,
        new DefaultFareService(),
        graph.getVehicleParkingService(),
        new DefaultVehicleRentalService(),
        new DefaultRealtimeVehicleService(transitService),
        GraphFinder.getInstance(graph, transitService::findRegularStops),
        new RouteRequest()
      );
  }

  @Test
  void testBasicBikePreferences() {
    var bicycleArgs = createArgsCopy(args);
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
    var env = executionContext(bicycleArgs, Locale.ENGLISH, context);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, context);
    var bikePreferences = routeRequest.preferences().bike();
    assertEquals(reluctance, bikePreferences.reluctance());
    assertEquals(speed, bikePreferences.speed());
    assertEquals(boardCost.toSeconds(), bikePreferences.boardCost());
  }

  @Test
  void testBikeWalkPreferences() {
    var bicycleArgs = createArgsCopy(args);
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
    var env = executionContext(bicycleArgs, Locale.ENGLISH, context);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, context);
    var bikeWalkingPreferences = routeRequest.preferences().bike().walking();
    assertEquals(walkSpeed, bikeWalkingPreferences.speed());
    assertEquals(mountDismountTime, bikeWalkingPreferences.mountDismountTime());
    assertEquals(mountDismountCost, bikeWalkingPreferences.mountDismountCost());
    assertEquals(walkReluctance, bikeWalkingPreferences.reluctance());
  }

  @Test
  void testBikeTrianglePreferences() {
    var bicycleArgs = createArgsCopy(args);
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
    var env = executionContext(bicycleArgs, Locale.ENGLISH, context);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, context);
    var bikePreferences = routeRequest.preferences().bike();
    assertEquals(VehicleRoutingOptimizeType.TRIANGLE, bikePreferences.optimizeType());
    var bikeTrianglePreferences = bikePreferences.optimizeTriangle();
    assertEquals(bikeSafety, bikeTrianglePreferences.safety());
    assertEquals(bikeFlatness, bikeTrianglePreferences.slope());
    assertEquals(bikeTime, bikeTrianglePreferences.time());
  }

  @Test
  void testBikeOptimizationPreferences() {
    var bicycleArgs = createArgsCopy(args);
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
    var env = executionContext(bicycleArgs, Locale.ENGLISH, context);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, context);
    var bikePreferences = routeRequest.preferences().bike();
    assertEquals(VehicleRoutingOptimizeType.SAFEST_STREETS, bikePreferences.optimizeType());
  }

  @Test
  void testBikeRentalPreferences() {
    var bicycleArgs = createArgsCopy(args);
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
    var env = executionContext(bicycleArgs, Locale.ENGLISH, context);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, context);
    var bikeRentalPreferences = routeRequest.preferences().bike().rental();
    assertEquals(allowed, bikeRentalPreferences.allowedNetworks());
    assertEquals(banned, bikeRentalPreferences.bannedNetworks());
    assertEquals(allowKeeping, bikeRentalPreferences.allowArrivingInRentedVehicleAtDestination());
    assertEquals(keepingCost, bikeRentalPreferences.arrivingInRentalVehicleAtDestinationCost());
  }

  @Test
  void testBikeParkingPreferences() {
    var bicycleArgs = createArgsCopy(args);
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
    var env = executionContext(bicycleArgs, Locale.ENGLISH, context);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, context);
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
