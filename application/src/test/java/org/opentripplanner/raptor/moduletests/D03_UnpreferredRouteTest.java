package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTripPattern.pattern;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;

import java.util.BitSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorProfile;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.raptor.moduletests.support.ModuleTestDebugLogging;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * FEATURE UNDER TEST
 * <p>
 * On transit options with identical cost, raptor should drop the unpreferred one which is modeled
 * by route penalty.
 */
public class D03_UnpreferredRouteTest implements RaptorTestConstants {

  private static final String EXPECTED =
    "Walk 30s ~ A ~ BUS %s 0:01 0:02:40 ~ B ~ Walk 20s " + "[0:00:30 0:03 2m30s Tₓ0 C₁%d]";
  private static final FeedScopedId ROUTE_ID_1 = TransitModelForTest.id("1");
  private static final FeedScopedId ROUTE_ID_2 = TransitModelForTest.id("2");
  private static final CostLinearFunction UNPREFERRED_C1 = CostLinearFunction.of("5m + 1t");
  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );

  @BeforeEach
  public void setup() {
    // Given 2 identical routes R1 and R2
    data.withRoute(
      route(pattern("R1", STOP_A, STOP_B).withRoute(TransitModelForTest.route(ROUTE_ID_1).build()))
        .withTimetable(schedule("00:01, 00:02:40"))
    );
    data.withRoute(
      route(pattern("R2", STOP_A, STOP_B).withRoute(TransitModelForTest.route(ROUTE_ID_2).build()))
        .withTimetable(schedule("00:01, 00:02:40"))
    );

    requestBuilder
      .searchParams()
      .addAccessPaths(TestAccessEgress.walk(STOP_A, D30s))
      .addEgressPaths(TestAccessEgress.walk(STOP_B, D20s))
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T00_10)
      .timetable(true);

    requestBuilder.profile(RaptorProfile.MULTI_CRITERIA);

    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  @Test
  public void unpreferR1() {
    unpreferRoute(ROUTE_ID_1);

    var request = requestBuilder.build();
    var response = raptorService.route(request, data);

    // Verify R1 is preferred and the cost is correct
    assertEquals(expected("R2", 800), pathsToString(response));
  }

  @Test
  public void unpreferR2() {
    unpreferRoute(ROUTE_ID_2);

    var request = requestBuilder.build();
    var response = raptorService.route(request, data);

    assertEquals(expected("R1", 800), pathsToString(response));
  }

  private void unpreferRoute(FeedScopedId routeId) {
    final BitSet patterns = new BitSet();
    for (var pattern : data.getPatterns()) {
      if (pattern.route().getId().equals(routeId)) {
        patterns.set(pattern.patternIndex());
      }
    }
    data.mcCostParamsBuilder().unpreferredPatterns(patterns);
    data.mcCostParamsBuilder().unpreferredCost(UNPREFERRED_C1);
  }

  private static String expected(String route, int cost) {
    return String.format(EXPECTED, route, cost);
  }
}
