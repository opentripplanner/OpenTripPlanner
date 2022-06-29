package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import static graphql.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.RouteCostCalculator.DEFAULT_ROUTE_RELUCTANCE;
import static org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.RouteCostCalculator.UNPREFERRED_ROUTE_RELUCTANCE;
import static org.opentripplanner.transit.model._data.TransitModelForTest.id;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.McCostParamsMapper;
import org.opentripplanner.routing.api.request.RequestFunctions;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransferConstraint;

public class RouteCostCalculatorTest {

  private static final int BOARD_COST_SEC = 5;
  private static final int TRANSFER_COST_SEC = 2;
  private static final double WAIT_RELUCTANCE_FACTOR = 0.5;
  private static final int TRANSIT_TIME = 1000;
  static final int UNPREFERRED_ROUTE_PENALTY = 300;
  private static final FeedScopedId UNPREFERRED_ROUTE_ID = id("999");
  private static final FeedScopedId DEFAULT_ROUTE_ID = id("101");

  private final DefaultCostCalculator<TestTripSchedule> defaultCostCalculator = new DefaultCostCalculator(
    BOARD_COST_SEC,
    TRANSFER_COST_SEC,
    WAIT_RELUCTANCE_FACTOR,
    null,
    null
  );

  private final RouteCostCalculator<TestTripSchedule> routeCostCalculator = createRouteCostCalculator();
  private final TestTripSchedule.Builder scheduleBuilder = TestTripSchedule.schedule("12:00 12:01");

  @Test
  @DisplayName("traversing an unpreferred route should add an extra cost")
  public void testUnpreferredRouteArrivalCostCalculation() {
    var schedule = scheduleBuilder.routeId(UNPREFERRED_ROUTE_ID).build();
    int defaultCost = calculateTransitArrivalCost(schedule, defaultCostCalculator);
    int routeArrivalCost = calculateTransitArrivalCost(schedule, routeCostCalculator);

    var expected = RaptorCostConverter.toRaptorCost(
      UNPREFERRED_ROUTE_PENALTY + TRANSIT_TIME * ((Double) UNPREFERRED_ROUTE_RELUCTANCE).intValue()
    );
    assertEquals(expected, routeArrivalCost - defaultCost);
  }

  @Test
  @DisplayName("traversing route 101 should not add any extra cost")
  public void testNormalRouteArrivalCostCalculation() {
    var schedule = scheduleBuilder.routeId(DEFAULT_ROUTE_ID).build();
    int defaultCost = calculateTransitArrivalCost(schedule, defaultCostCalculator);
    int routeArrivalCost = calculateTransitArrivalCost(schedule, routeCostCalculator);

    assertEquals(defaultCost, routeArrivalCost);
  }

  // TODO: eject parametrized test cases to 2 simple ones

  @Test
  @DisplayName("boarding should not add any cost")
  public void testBoardCostRemainsConstant() {
    var schedule = scheduleBuilder.routeId(DEFAULT_ROUTE_ID).build();

    int defaultCost = calculateBoardingCost(schedule, defaultCostCalculator);
    int routeBoardCost = calculateBoardingCost(schedule, routeCostCalculator);

    assertEquals(defaultCost, routeBoardCost);
  }

  @Test
  @DisplayName("cost mapper should create penalty map")
  public void testMcCostParameterMapping() {
    RoutingRequest routingRequest = new RoutingRequest();

    routingRequest.setUnpreferredRoutes(List.of(UNPREFERRED_ROUTE_ID));
    var costFunctionString = String.format("%d + 1.0 x", UNPREFERRED_ROUTE_PENALTY);
    routingRequest.setUnpreferredRouteCost(costFunctionString);

    McCostParams costParams = McCostParamsMapper.map(routingRequest);

    var routes = costParams.unpreferredRoutes();

    // linear cost function
    double expected = (double) UNPREFERRED_ROUTE_PENALTY + TRANSIT_TIME * DEFAULT_ROUTE_RELUCTANCE;
    double actual = costParams.unnpreferredCost().apply(TRANSIT_TIME);

    assertTrue(routes.contains(UNPREFERRED_ROUTE_ID));
    assertFalse(routes.contains(DEFAULT_ROUTE_ID));
    assertEquals(expected, actual);
  }

  private int calculateTransitArrivalCost(DefaultTripSchedule schedule, CostCalculator calc) {
    int boardCost = calculateBoardingCost(schedule, calc);
    return calc.transitArrivalCost(boardCost, 0, TRANSIT_TIME, schedule, 6);
  }

  private int calculateBoardingCost(DefaultTripSchedule schedule, CostCalculator calc) {
    return calc.boardingCost(true, 0, 5, 100, schedule, RaptorTransferConstraint.REGULAR_TRANSFER);
  }

  private RouteCostCalculator createRouteCostCalculator() {
    // create mock penalty map
    var costFunction = RequestFunctions.createLinearFunction(
      UNPREFERRED_ROUTE_PENALTY,
      UNPREFERRED_ROUTE_RELUCTANCE
    );
    return new RouteCostCalculator<>(
      defaultCostCalculator,
      Set.of(UNPREFERRED_ROUTE_ID),
      costFunction
    );
  }
}
