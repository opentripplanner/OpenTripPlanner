package org.opentripplanner.apis.gtfs.mapping.routerequest;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.apis.gtfs.mapping.routerequest.RouteRequestMapperTest.createArgsCopy;
import static org.opentripplanner.apis.gtfs.mapping.routerequest.RouteRequestMapperTest.executionContext;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.transit.model.basic.TransitMode;

class RouteRequestMapperTransitTest {

  @Test
  void testBoardPreferences() {
    var args = createArgsCopy(RouteRequestMapperTest.ARGS);
    var reluctance = 7.5;
    var slack = Duration.ofSeconds(125);
    args.put(
      "preferences",
      Map.ofEntries(
        entry(
          "transit",
          Map.ofEntries(
            entry(
              "board",
              Map.ofEntries(entry("waitReluctance", reluctance), entry("slack", slack))
            )
          )
        )
      )
    );
    var env = executionContext(args, Locale.ENGLISH, RouteRequestMapperTest.CONTEXT);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, RouteRequestMapperTest.CONTEXT);
    var transferPreferences = routeRequest.preferences().transfer();
    assertEquals(reluctance, transferPreferences.waitReluctance());
    var transitPreferences = routeRequest.preferences().transit();
    assertEquals(slack, transitPreferences.boardSlack().valueOf(TransitMode.BUS));
  }

  @Test
  void testAlightPreferences() {
    var args = createArgsCopy(RouteRequestMapperTest.ARGS);
    var slack = Duration.ofSeconds(125);
    args.put(
      "preferences",
      Map.ofEntries(
        entry("transit", Map.ofEntries(entry("alight", Map.ofEntries(entry("slack", slack)))))
      )
    );
    var env = executionContext(args, Locale.ENGLISH, RouteRequestMapperTest.CONTEXT);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, RouteRequestMapperTest.CONTEXT);
    var transitPreferences = routeRequest.preferences().transit();
    assertEquals(slack, transitPreferences.alightSlack().valueOf(TransitMode.BUS));
  }

  @Test
  void testTransferPreferences() {
    var args = createArgsCopy(RouteRequestMapperTest.ARGS);
    var cost = Cost.costOfSeconds(75);
    var slack = Duration.ofSeconds(125);
    var maximumAdditionalTransfers = 1;
    var maximumTransfers = 3;
    args.put(
      "preferences",
      Map.ofEntries(
        entry(
          "transit",
          Map.ofEntries(
            entry(
              "transfer",
              Map.ofEntries(
                entry("cost", cost),
                entry("slack", slack),
                entry("maximumAdditionalTransfers", maximumAdditionalTransfers),
                entry("maximumTransfers", maximumTransfers)
              )
            )
          )
        )
      )
    );
    var env = executionContext(args, Locale.ENGLISH, RouteRequestMapperTest.CONTEXT);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, RouteRequestMapperTest.CONTEXT);
    var transferPreferences = routeRequest.preferences().transfer();
    assertEquals(cost.toSeconds(), transferPreferences.cost());
    assertEquals(slack, transferPreferences.slack());
    assertEquals(maximumAdditionalTransfers, transferPreferences.maxAdditionalTransfers());
    assertEquals(maximumTransfers + 1, transferPreferences.maxTransfers());
  }

  @Test
  void testTimetablePreferences() {
    var args = createArgsCopy(RouteRequestMapperTest.ARGS);
    var excludeRealTimeUpdates = true;
    var includePlannedCancellations = true;
    var includeRealTimeCancellations = true;
    args.put(
      "preferences",
      Map.ofEntries(
        entry(
          "transit",
          Map.ofEntries(
            entry(
              "timetable",
              Map.ofEntries(
                entry("excludeRealTimeUpdates", excludeRealTimeUpdates),
                entry("includePlannedCancellations", includePlannedCancellations),
                entry("includeRealTimeCancellations", includeRealTimeCancellations)
              )
            )
          )
        )
      )
    );
    var env = executionContext(args, Locale.ENGLISH, RouteRequestMapperTest.CONTEXT);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, RouteRequestMapperTest.CONTEXT);
    var transitPreferences = routeRequest.preferences().transit();
    assertEquals(excludeRealTimeUpdates, transitPreferences.ignoreRealtimeUpdates());
    assertEquals(includePlannedCancellations, transitPreferences.includePlannedCancellations());
    assertEquals(includeRealTimeCancellations, transitPreferences.includeRealtimeCancellations());
  }
}
