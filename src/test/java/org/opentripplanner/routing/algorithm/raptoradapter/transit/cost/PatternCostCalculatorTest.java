package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import static graphql.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.PatternCostCalculator.UNPREFERRED_ROUTE_RELUCTANCE;
import static org.opentripplanner.transit.model._data.TransitModelForTest.agency;
import static org.opentripplanner.transit.model._data.TransitModelForTest.id;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripPattern;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.model.RaptorTransferConstraint;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.GeneralizedCostParametersMapper;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.framework.DoubleAlgorithmFunction;
import org.opentripplanner.routing.api.request.framework.RequestFunctions;
import org.opentripplanner.test.support.VariableSource;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.RouteBuilder;
import org.opentripplanner.transit.model.organization.Agency;

public class PatternCostCalculatorTest {

  private static final int BOARD_COST_SEC = 5;
  private static final int TRANSFER_COST_SEC = 2;
  private static final double WAIT_RELUCTANCE_FACTOR = 0.5;
  private static final int TRANSIT_TIME = 1000;
  private static final double UNPREFERRED_ROUTE_PENALTY = 300.0;
  private static final FeedScopedId UNPREFERRED_ROUTE_ID = id("999");
  private static final FeedScopedId UNPREFERRED_AGENCY_ID = id("contoso-travels");
  private static final Agency UNPREFERRED_AGENCY = agency(UNPREFERRED_AGENCY_ID.getId());
  private static final FeedScopedId DEFAULT_ROUTE_ID = id("101");
  // Default cost function: a + bx
  private static final DoubleAlgorithmFunction unprefCostFn = RequestFunctions.createLinearFunction(
    RaptorCostConverter.toRaptorCost(UNPREFERRED_ROUTE_PENALTY),
    RaptorCostConverter.toRaptorCost(UNPREFERRED_ROUTE_RELUCTANCE)
  );

  protected static final DefaultCostCalculator<TestTripSchedule> defaultCostCalculator = new DefaultCostCalculator<>(
    BOARD_COST_SEC,
    TRANSFER_COST_SEC,
    WAIT_RELUCTANCE_FACTOR,
    null,
    null
  );

  static Stream<Arguments> testCases = Stream
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
    .map(Arguments::of);

  @Test
  @DisplayName("cost mapper should create penalty map")
  public void testMcCostParameterMapping() {
    RouteRequest routingRequest = new RouteRequest();

    routingRequest.journey().transit().setUnpreferredRoutes(List.of(UNPREFERRED_ROUTE_ID));
    routingRequest.journey().transit().setUnpreferredAgencies(List.of(UNPREFERRED_AGENCY_ID));
    routingRequest.withPreferences(p ->
      p.withTransit(tr -> tr.setUnpreferredCost(RequestFunctions.parse("300 + 1.0 x")))
    );

    var data = new TestTransitData();
    final TestTripPattern unpreferredRoutePattern = pattern(true, false);
    final TestTripPattern unpreferredAgencyPattern = pattern(false, true);
    final TestTripPattern unpreferredRouteAgencyPattern = pattern(false, true);
    final TestTripPattern defaultPattern = pattern(false, false);

    data.withRoutes(
      route(unpreferredRoutePattern),
      route(unpreferredAgencyPattern),
      route(unpreferredRouteAgencyPattern),
      route(defaultPattern)
    );

    GeneralizedCostParameters costParams = GeneralizedCostParametersMapper.map(
      routingRequest,
      data.getPatterns()
    );
    var unpreferredPatterns = costParams.unpreferredPatterns();

    assertTrue(unpreferredPatterns.get(unpreferredRoutePattern.patternIndex()));
    assertTrue(unpreferredPatterns.get(unpreferredAgencyPattern.patternIndex()));
    assertTrue(unpreferredPatterns.get(unpreferredRouteAgencyPattern.patternIndex()));
    assertFalse(unpreferredPatterns.get(defaultPattern.patternIndex()));

    // test creation of linear cost function
    double expected = 300 + 1.0 * TRANSIT_TIME;
    double actual = costParams.unnpreferredCost().calculate(TRANSIT_TIME);

    assertEquals(expected, actual);
  }

  @ParameterizedTest(name = "should apply penalty for scenario: {0}")
  @VariableSource("testCases")
  public void testPreferencesPenaltyForRoute(String testCaseDescription) {
    RoutePenaltyTC tc = new RoutePenaltyTC(testCaseDescription);

    var schedule = tc.createSchedule();
    var costCalculator = tc.createCostCalculator(schedule);

    int defaultArrCost = transitArrivalCost(schedule, defaultCostCalculator);
    int defaultBoardCost = boardingCost(schedule, defaultCostCalculator);
    int arrCost = transitArrivalCost(schedule, costCalculator);
    int boardCost = boardingCost(schedule, costCalculator);

    // if we either have just unpreferred routes or just unpreferred agencies
    if (tc.unPrefRoute || tc.unPrefAgency) {
      var expectedArr = unprefCostFn.calculate(TRANSIT_TIME);
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
    // TODO: preferred route
    // TODO: preferred agency
  }

  private int transitArrivalCost(
    TestTripSchedule schedule,
    RaptorCostCalculator<TestTripSchedule> calc
  ) {
    int boardCost = boardingCost(schedule, calc);
    return calc.transitArrivalCost(boardCost, 0, TRANSIT_TIME, schedule, 6);
  }

  private int boardingCost(TestTripSchedule schedule, RaptorCostCalculator<TestTripSchedule> calc) {
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

      return "RoutePenaltyTC {" + sb.substring(sb.isEmpty() ? 0 : 2) + "}";
    }

    boolean isDefault() {
      return !(prefAgency || prefRoute || unPrefAgency || unPrefRoute);
    }

    RaptorCostCalculator<TestTripSchedule> createCostCalculator(TestTripSchedule schedule) {
      GeneralizedCostParameters costParams = GeneralizedCostParametersMapper.map(
        createRouteRequest(),
        List.of(schedule.pattern())
      );

      return CostCalculatorFactory.createCostCalculator(costParams, null);
    }

    /**
     * Create a TripSchedule for initialized scenario for testing penalties with RouteCostCalculator
     * implementation.
     *
     * @return Test schedule
     */
    TestTripSchedule createSchedule() {
      return TestTripSchedule
        .schedule("12:00 12:01")
        .pattern(pattern(unPrefRoute, unPrefAgency))
        .build();
    }

    RouteRequest createRouteRequest() {
      var request = new RouteRequest();

      request.withPreferences(preferences -> {
        preferences.withTransit(transit ->
          transit.setUnpreferredCost(
            RequestFunctions.createLinearFunction(
              UNPREFERRED_ROUTE_PENALTY,
              UNPREFERRED_ROUTE_RELUCTANCE
            )
          )
        );
        preferences.withWalk(w -> w.withBoardCost(BOARD_COST_SEC));
        preferences.withTransfer(tx -> {
          tx.withCost(TRANSFER_COST_SEC).withWaitReluctance(WAIT_RELUCTANCE_FACTOR);
        });
      });

      if (prefAgency) {
        // TODO
        request.journey().transit().setUnpreferredAgencies(List.of());
      }
      if (prefRoute) {
        // TODO
        request.journey().transit().setUnpreferredRoutes(List.of());
      }
      if (unPrefAgency) {
        request.journey().transit().setUnpreferredAgencies(List.of(UNPREFERRED_AGENCY_ID));
      }
      if (unPrefRoute) {
        request.journey().transit().setUnpreferredRoutes(List.of(UNPREFERRED_ROUTE_ID));
      }
      return request;
    }
  }

  private static TestTripPattern pattern(boolean unpreferredRoute, boolean unpreferredAgency) {
    RouteBuilder builder = TransitModelForTest.route(
      unpreferredRoute ? UNPREFERRED_ROUTE_ID : DEFAULT_ROUTE_ID
    );

    if (unpreferredAgency) {
      builder.withAgency(UNPREFERRED_AGENCY);
    }

    return TestTripPattern.pattern(1, 2).withRoute(builder.build());
  }
}
