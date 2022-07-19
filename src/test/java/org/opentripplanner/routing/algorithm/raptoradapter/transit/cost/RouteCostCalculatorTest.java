package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import static graphql.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.RouteCostCalculator.UNPREFERRED_ROUTE_RELUCTANCE;
import static org.opentripplanner.transit.model._data.TransitModelForTest.id;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.DoubleFunction;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.McCostParamsMapper;
import org.opentripplanner.routing.api.request.RequestFunctions;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.test.support.VariableSource;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransferConstraint;

public class RouteCostCalculatorTest {

  private static final int BOARD_COST_SEC = 5;
  private static final int TRANSFER_COST_SEC = 2;
  private static final double WAIT_RELUCTANCE_FACTOR = 0.5;
  private static final int TRANSIT_TIME = 1000;
  private static final double UNPREFERRED_ROUTE_PENALTY = 300.0;
  private static final FeedScopedId UNPREFERRED_ROUTE_ID = id("999");
  private static final FeedScopedId OTHER_ROUTE_ID = id("X");
  private static final FeedScopedId DEFAULT_ROUTE_ID = id("101");
  private static final FeedScopedId AGENCY_ID = id("A1");
  // Default cost function: a + bx
  private static final DoubleFunction<Double> unprefCostFn = RequestFunctions.createLinearFunction(
    RaptorCostConverter.toRaptorCost(UNPREFERRED_ROUTE_PENALTY),
    RaptorCostConverter.toRaptorCost(UNPREFERRED_ROUTE_RELUCTANCE)
  );

  protected static final DefaultCostCalculator<TestTripSchedule> defaultCostCalculator = new DefaultCostCalculator(
    BOARD_COST_SEC,
    TRANSFER_COST_SEC,
    WAIT_RELUCTANCE_FACTOR,
    null,
    null
  );

  static Stream<Arguments> testCases = List
    .of(
      // !prefAgency | !prefRoute | unPrefA | unPrefR | expected cost
      "       -      |      -     |    -    |    -    |     0",
      "       -      |      -     |    -    |    x    |   300",
      "       -      |      -     |    x    |    -    |   300",
      "       -      |      x     |    -    |    -    |   300",
      "       x      |      -     |    -    |    -    |   300",
      "       -      |      -     |    x    |    x    |   300",
      "       x      |      x     |    -    |    -    |   300",
      "       x      |      -     |    -    |    x    |   600",
      "       -      |      x     |    x    |    -    |   600",
      "       x      |      x     |    x    |    x    |   600"
    )
    .stream()
    .map(Arguments::of);

  @Test
  @DisplayName("cost mapper should create penalty map")
  public void testMcCostParameterMapping() {
    RoutingRequest routingRequest = new RoutingRequest();
    routingRequest.setUnpreferredRoutes(List.of(UNPREFERRED_ROUTE_ID));
    routingRequest.setUnpreferredRouteCost("300 + 1.0 x");

    McCostParams costParams = McCostParamsMapper.map(routingRequest);
    var routes = costParams.unpreferredRoutes();

    assertTrue(routes.contains(UNPREFERRED_ROUTE_ID));
    assertFalse(routes.contains(DEFAULT_ROUTE_ID));

    // test creation of linear cost function
    double expected = (double) 300 + 1.0 * TRANSIT_TIME;
    double actual = costParams.unnpreferredCost().apply(TRANSIT_TIME);

    assertEquals(expected, actual);
  }

  @ParameterizedTest(name = "should apply penalty for scenario: {0}")
  @VariableSource("testCases")
  public void testPreferencesPenaltyForRoute(String testCaseDescription) {
    RoutePenaltyTC tc = new RoutePenaltyTC(testCaseDescription);

    var schedule = tc.createSchedule();
    var routeCostCalculator = tc.createRouteCostCalculator();

    int defaultArrCost = transitArrivalCost(schedule, defaultCostCalculator);
    int defaultBoardCost = boardingCost(schedule, defaultCostCalculator);
    int arrCost = transitArrivalCost(schedule, routeCostCalculator);
    int boardCost = boardingCost(schedule, routeCostCalculator);

    if (tc.unPrefRoute) {
      var expectedArr = unprefCostFn.apply(TRANSIT_TIME);
      var errorMessageArr = String.format("Invalid arrival cost: %s", tc);
      assertEquals(expectedArr, arrCost - defaultArrCost, errorMessageArr);
      /*
      TODO: separate boarding cost calculation can be tested when boarding and arrival cost
            constants are differentiated
      var expectedBoard = unprefCostFn.apply(0);
      var errorMessageBoard = String.format("Invalid boarding cost: %s", tc);
      assertEquals(expectedBoard, boardCost - defaultBoardCost, errorMessageBoard);
      */
    }

    if (tc.isDefault()) {
      // expect default penalties
      assertEquals(defaultArrCost, arrCost);
      assertEquals(defaultBoardCost, boardCost);
    }
    // TODO: unpreferred agency
    // TODO: preferred route
    // TODO: preferred agency
  }

  private int transitArrivalCost(DefaultTripSchedule schedule, CostCalculator calc) {
    int boardCost = boardingCost(schedule, calc);
    return calc.transitArrivalCost(boardCost, 0, TRANSIT_TIME, schedule, 6);
  }

  private int boardingCost(DefaultTripSchedule schedule, CostCalculator calc) {
    return calc.boardingCost(true, 0, 5, 100, schedule, RaptorTransferConstraint.REGULAR_TRANSFER);
  }

  private static class RoutePenaltyTC {

    final boolean prefAgency;
    final boolean prefRoute;
    final boolean unPrefAgency;
    final boolean unPrefRoute;
    public final int expectedCost;

    RoutePenaltyTC(String input) {
      String[] cells = input.replace(" ", "").split("\\|");
      this.prefAgency = "x".equalsIgnoreCase(cells[0]);
      this.prefRoute = "x".equalsIgnoreCase(cells[1]);
      this.unPrefAgency = "x".equalsIgnoreCase(cells[2]);
      this.unPrefRoute = "x".equalsIgnoreCase(cells[3]);
      this.expectedCost = Integer.parseInt(cells[4]);
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder();
      if (prefAgency) {
        sb.append(", prefAgency=X");
      }
      if (prefRoute) {
        sb.append(", prefRoute=X");
      }
      if (unPrefAgency) {
        sb.append(", unPrefAgency=X");
      }
      if (unPrefRoute) {
        sb.append(", unPrefRoute=X");
      }

      return "RoutePenaltyTC {" + sb.substring(sb.length() == 0 ? 0 : 2) + "}";
    }

    boolean isDefault() {
      return !(prefAgency || prefRoute || unPrefAgency || unPrefRoute);
    }

    RouteCostCalculator createRouteCostCalculator() {
      var unprefCostFn = RequestFunctions.createLinearFunction(
        UNPREFERRED_ROUTE_PENALTY,
        UNPREFERRED_ROUTE_RELUCTANCE
      );
      Set<FeedScopedId> unprefRouteIds = new HashSet();

      if (unPrefRoute) {
        unprefRouteIds.add(UNPREFERRED_ROUTE_ID);
      }

      return new RouteCostCalculator<>(defaultCostCalculator, unprefRouteIds, unprefCostFn);
    }

    /**
     * Create a TripSchedule for initialized scenario for testing penalties with RouteCostCalculator
     * implementation.
     *
     * @return Test schedule
     */
    TestTripSchedule createSchedule() {
      var scheduleBuilder = TestTripSchedule.schedule("12:00 12:01");

      if (unPrefRoute) {
        return scheduleBuilder.routeId(UNPREFERRED_ROUTE_ID).build();
      }

      return scheduleBuilder.build();
    }

    RoutingRequest createRoutingRequest() {
      RoutingRequest request = new RoutingRequest();
      if (prefAgency) {
        request.setPreferredAgencies(List.of(OTHER_ROUTE_ID));
      }
      if (prefRoute) {
        request.setPreferredRoutes(List.of(OTHER_ROUTE_ID));
      }
      if (unPrefAgency) {
        request.setUnpreferredAgencies(List.of(AGENCY_ID));
      }
      if (unPrefRoute) {
        request.setUnpreferredRoutes(List.of(UNPREFERRED_ROUTE_ID));
      }
      return request;
    }
  }
}
