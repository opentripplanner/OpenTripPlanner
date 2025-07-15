package org.opentripplanner.apis.gtfs.mapping.routerequest;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.model.Cost;

class RouteRequestMapperCarTest {

  private final _RouteRequestTestContext testCtx = _RouteRequestTestContext.of(Locale.ENGLISH);

  @Test
  void testBasicCarPreferences() {
    var carArgs = testCtx.basicRequest();
    var reluctance = 7.5;
    var boardCost = Cost.costOfSeconds(500);
    carArgs.put(
      "preferences",
      Map.ofEntries(
        entry(
          "street",
          Map.ofEntries(
            entry(
              "car",
              Map.ofEntries(entry("reluctance", reluctance), entry("boardCost", boardCost))
            )
          )
        )
      )
    );
    var env = testCtx.executionContext(carArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, testCtx.context());
    var carPreferences = routeRequest.preferences().car();
    assertEquals(reluctance, carPreferences.reluctance());
    assertEquals(boardCost.toSeconds(), carPreferences.boardCost());
  }

  @Test
  void testCarRentalPreferences() {
    var carArgs = testCtx.basicRequest();
    var allowed = Set.of("foo", "bar");
    var banned = Set.of("not");
    carArgs.put(
      "preferences",
      Map.ofEntries(
        entry(
          "street",
          Map.ofEntries(
            entry(
              "car",
              Map.ofEntries(
                entry(
                  "rental",
                  Map.ofEntries(
                    entry("allowedNetworks", allowed.stream().toList()),
                    entry("bannedNetworks", banned.stream().toList())
                  )
                )
              )
            )
          )
        )
      )
    );
    var env = testCtx.executionContext(carArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, testCtx.context());
    var carRentalPreferences = routeRequest.preferences().car().rental();
    assertEquals(allowed, carRentalPreferences.allowedNetworks());
    assertEquals(banned, carRentalPreferences.bannedNetworks());
  }

  @Test
  void testEmptyCarRentalPreferences() {
    var carArgs = testCtx.basicRequest();
    var empty = Set.of();
    carArgs.put(
      "preferences",
      Map.ofEntries(
        entry(
          "street",
          Map.ofEntries(
            entry(
              "car",
              Map.ofEntries(
                entry("rental", Map.ofEntries(entry("allowedNetworks", empty.stream().toList())))
              )
            )
          )
        )
      )
    );
    var allowedEnv = testCtx.executionContext(carArgs);
    assertThrows(IllegalArgumentException.class, () ->
      RouteRequestMapper.toRouteRequest(allowedEnv, testCtx.context())
    );

    carArgs = testCtx.basicRequest();
    carArgs.put(
      "preferences",
      Map.ofEntries(
        entry(
          "street",
          Map.ofEntries(
            entry(
              "car",
              Map.ofEntries(
                entry("rental", Map.ofEntries(entry("bannedNetworks", empty.stream().toList())))
              )
            )
          )
        )
      )
    );
    var bannedEnv = testCtx.executionContext(carArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(bannedEnv, testCtx.context());
    var carRentalPreferences = routeRequest.preferences().car().rental();
    assertEquals(empty, carRentalPreferences.bannedNetworks());
  }

  @Test
  void testCarParkingPreferences() {
    var carArgs = testCtx.basicRequest();
    var unpreferredCost = Cost.costOfSeconds(150);
    var notFilter = List.of("wheelbender");
    var selectFilter = List.of("locker", "roof");
    var unpreferred = List.of("bad");
    var preferred = List.of("a", "b");
    carArgs.put(
      "preferences",
      Map.ofEntries(
        entry(
          "street",
          Map.ofEntries(
            entry(
              "car",
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
    var env = testCtx.executionContext(carArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, testCtx.context());
    var carParkingPreferences = routeRequest.preferences().car().parking();
    assertEquals(unpreferredCost, carParkingPreferences.unpreferredVehicleParkingTagCost());
    assertEquals(
      "VehicleParkingFilter{not: [tags=%s], select: [tags=%s]}".formatted(notFilter, selectFilter),
      carParkingPreferences.filter().toString()
    );
    assertEquals(
      "VehicleParkingFilter{not: [tags=%s], select: [tags=%s]}".formatted(unpreferred, preferred),
      carParkingPreferences.preferred().toString()
    );
  }
}
