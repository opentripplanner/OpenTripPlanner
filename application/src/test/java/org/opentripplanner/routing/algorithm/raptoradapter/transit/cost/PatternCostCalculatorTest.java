package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import static graphql.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.raptorlegacy._data.transit.TestRoute.route;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.agency;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.raptor.api.model.RaptorTransferConstraint;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.raptorlegacy._data.transit.TestTransitData;
import org.opentripplanner.raptorlegacy._data.transit.TestTripPattern;
import org.opentripplanner.raptorlegacy._data.transit.TestTripSchedule;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.GeneralizedCostParametersMapper;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;
import org.opentripplanner.test.support.TestTableParser;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.RouteBuilder;
import org.opentripplanner.transit.model.organization.Agency;

public class PatternCostCalculatorTest {

  private static final int BOARD_COST_SEC = 5;
  private static final int TRANSFER_COST_SEC = 2;
  private static final double WAIT_RELUCTANCE_FACTOR = 0.5;
  private static final int TRANSIT_TIME = 1000;
  private static final Duration UNPREFERRED_ROUTE_PENALTY = Duration.ofMinutes(5);
  private static final double UNPREFERRED_ROUTE_RELUCTANCE = 2.0;
  private static final FeedScopedId DEFAULT_ROUTE_ID = id("101");
  private static final FeedScopedId UNPREFERRED_ROUTE_ID = id("999");
  private static final FeedScopedId UNPREFERRED_AGENCY_ID = id("contoso-travels");
  private static final Agency UNPREFERRED_AGENCY = agency(UNPREFERRED_AGENCY_ID.getId());

  protected static final RaptorCostCalculator<TestTripSchedule> DEFAULT_COST_CALCULATOR =
    new DefaultCostCalculator<>(
      BOARD_COST_SEC,
      TRANSFER_COST_SEC,
      WAIT_RELUCTANCE_FACTOR,
      null,
      null
    );

  @Test
  @DisplayName("cost mapper should create penalty map")
  public void testMcCostParameterMapping() {
    var unpreferredCostFunctionOtpDomain = CostLinearFunction.of("5m + 1.1 t");
    RouteRequest routingRequest = RouteRequest.of()
      .withJourney(jb ->
        jb.withTransit(b -> {
          b.withUnpreferredRoutes(List.of(UNPREFERRED_ROUTE_ID));
          b.withUnpreferredAgencies(List.of(UNPREFERRED_AGENCY_ID));
        })
      )
      .withPreferences(p ->
        p.withTransit(tr -> tr.setUnpreferredCost(unpreferredCostFunctionOtpDomain))
      )
      .buildDefault();

    var data = new TestTransitData();
    final TestTripPattern defaultPattern = pattern(false, false);
    final TestTripPattern unpreferredRoutePattern = pattern(true, false);
    final TestTripPattern unpreferredAgencyPattern = pattern(false, true);
    final TestTripPattern unpreferredRouteAgencyPattern = pattern(false, true);

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

    // test creation of linear cost function, the cost is in Raptor centi-seconds
    double expected = unpreferredCostFunctionOtpDomain
      .calculate(Cost.costOfSeconds(TRANSIT_TIME))
      .toCentiSeconds();
    double actual = costParams.unnpreferredCost().calculateRaptorCost(TRANSIT_TIME);
    assertEquals(expected, actual);
  }

  private static Stream<Arguments> preferencesPenaltyForRouteTestCases() {
    return TestTableParser.of(
      """
      #    unpreferred    |  expected
      #  agency | route   | extra-cost
           -    |    -    |     0
           -    |    x    |   230000
           x    |    -    |   230000
           x    |    x    |   230000
      """
    );
  }

  @ParameterizedTest(
    name = "pref (agency|route) | unPref (agency|route): {0} | {1} | {2} | {3} || {4}"
  )
  @MethodSource("preferencesPenaltyForRouteTestCases")
  public void testPreferencesPenaltyForRoute(
    boolean unPrefAgency,
    boolean unPrefRoute,
    int expectedExtraArrivalCost
  ) {
    RoutePenaltyTC tc = new RoutePenaltyTC(unPrefAgency, unPrefRoute, expectedExtraArrivalCost);

    var schedule = tc.createSchedule();
    var costCalculator = tc.createCostCalculator(schedule);

    int defaultBoardCost = boardingCost(schedule, DEFAULT_COST_CALCULATOR);
    int boardCost = boardingCost(schedule, costCalculator);
    assertEquals(defaultBoardCost, boardCost);

    int defaultArrCost = transitArrivalCost(schedule, DEFAULT_COST_CALCULATOR);
    int arrCost = transitArrivalCost(schedule, costCalculator);
    assertEquals(defaultArrCost + expectedExtraArrivalCost, arrCost);
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

  private record RoutePenaltyTC(
    boolean unPreferredAgency,
    boolean unPreferredRoute,
    int expectedCost
  ) {
    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder();
      if (unPreferredAgency) {
        sb.append(", unPrefAgency=X");
      }
      if (unPreferredRoute) {
        sb.append(", unPrefRoute=X");
      }

      return "RoutePenaltyTC {" + sb.substring(sb.isEmpty() ? 0 : 2) + "}";
    }

    boolean isDefault() {
      return !(unPreferredAgency || unPreferredRoute);
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
      return TestTripSchedule.schedule("12:00 12:01")
        .pattern(pattern(unPreferredRoute, unPreferredAgency))
        .build();
    }

    RouteRequest createRouteRequest() {
      return RouteRequest.of()
        .withPreferences(preferences -> {
          preferences.withTransit(transit ->
            transit.setUnpreferredCost(
              CostLinearFunction.of(UNPREFERRED_ROUTE_PENALTY, UNPREFERRED_ROUTE_RELUCTANCE)
            )
          );
          preferences.withWalk(w -> w.withBoardCost(BOARD_COST_SEC));
          preferences.withTransfer(tx -> {
            tx.withCost(TRANSFER_COST_SEC).withWaitReluctance(WAIT_RELUCTANCE_FACTOR);
          });
        })
        .withJourney(jb ->
          jb.withTransit(b -> {
            if (unPreferredAgency) {
              b.withUnpreferredAgencies(List.of(UNPREFERRED_AGENCY_ID));
            }
            if (unPreferredRoute) {
              b.withUnpreferredRoutes(List.of(UNPREFERRED_ROUTE_ID));
            }
          })
        )
        .buildDefault();
    }
  }

  private static TestTripPattern pattern(boolean unpreferredRoute, boolean unpreferredAgency) {
    RouteBuilder builder = TimetableRepositoryForTest.route(
      unpreferredRoute ? UNPREFERRED_ROUTE_ID : DEFAULT_ROUTE_ID
    );

    if (unpreferredAgency) {
      builder.withAgency(UNPREFERRED_AGENCY);
    }

    return TestTripPattern.pattern(1, 2).withRoute(builder.build());
  }
}
