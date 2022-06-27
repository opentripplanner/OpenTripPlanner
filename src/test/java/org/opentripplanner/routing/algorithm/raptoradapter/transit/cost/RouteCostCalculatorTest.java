package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.model._data.TransitModelForTest.id;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.McCostParamsMapper;
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

  static Stream<Arguments> testCases = Stream.of(
    Arguments.of(DEFAULT_ROUTE_ID, 0),
    Arguments.of(UNPREFERRED_ROUTE_ID, UNPREFERRED_ROUTE_PENALTY)
  );

  @ParameterizedTest(name = "traversing route '{0}' should add an extra cost of {1}")
  @VariableSource("testCases")
  public void calculateExtraArrivalCost(FeedScopedId routeId, int expectedExtraCost) {
    var schedule = scheduleBuilder.routeId(routeId).build();
    int routeArrivalCost = calculateTransitArrivalCost(schedule, routeCostCalculator);

    assertEquals(expectedCost(schedule, expectedExtraCost), routeArrivalCost);
  }

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

    McCostParams costParams = McCostParamsMapper.map(routingRequest);

    var map = costParams.routePenalties();

    Integer penaltyUnprefer = map.getOrDefault(UNPREFERRED_ROUTE_ID, 0);
    Integer penaltyNone = map.getOrDefault(DEFAULT_ROUTE_ID, 0);
    Integer expected = RaptorCostConverter.toRaptorCost(routingRequest.useUnpreferredRoutesPenalty);

    assertEquals(expected, penaltyUnprefer);
    assertEquals(0, penaltyNone);
  }

  private int expectedCost(DefaultTripSchedule schedule, int expectedExtra) {
    int defaultCost = calculateTransitArrivalCost(schedule, defaultCostCalculator);
    return defaultCost + RaptorCostConverter.toRaptorCost(expectedExtra);
  }

  private int calculateTransitArrivalCost(DefaultTripSchedule schedule, CostCalculator calc) {
    int boardCost = calculateBoardingCost(schedule, calc);
    return calc.transitArrivalCost(boardCost, 0, 1000, schedule, 6);
  }

  private int calculateBoardingCost(DefaultTripSchedule schedule, CostCalculator calc) {
    return calc.boardingCost(true, 0, 5, 100, schedule, RaptorTransferConstraint.REGULAR_TRANSFER);
  }

  private RouteCostCalculator createRouteCostCalculator() {
    // create mock penalty map
    Map<FeedScopedId, Integer> penaltyMap = Map.of(
      UNPREFERRED_ROUTE_ID,
      RaptorCostConverter.toRaptorCost(UNPREFERRED_ROUTE_PENALTY)
    );
    return new RouteCostCalculator<>(defaultCostCalculator, penaltyMap);
  }
}
