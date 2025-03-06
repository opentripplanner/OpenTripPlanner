package org.opentripplanner.routing.algorithm.transferoptimization.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.api.path.EgressPathLeg;
import org.opentripplanner.raptor.api.path.TransitPathLeg;
import org.opentripplanner.raptor.spi.DefaultSlackProvider;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.raptor.spi.RaptorSlackProvider;
import org.opentripplanner.raptorlegacy._data.RaptorTestConstants;
import org.opentripplanner.raptorlegacy._data.transit.TestAccessEgress;
import org.opentripplanner.raptorlegacy._data.transit.TestTripSchedule;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.DefaultCostCalculator;
import org.opentripplanner.routing.algorithm.transferoptimization.model.OptimizedPathTail;
import org.opentripplanner.routing.algorithm.transferoptimization.model.PathTailFilter;
import org.opentripplanner.routing.algorithm.transferoptimization.model.costfilter.MinCostPathTailFilterFactory;
import org.opentripplanner.utils.time.TimeUtils;

public class TransitPathLegSelectorTest implements RaptorTestConstants {

  private static final RaptorSlackProvider SLACK_PROVIDER = new DefaultSlackProvider(
    TRANSFER_SLACK,
    BOARD_SLACK,
    ALIGHT_SLACK
  );

  private static final RaptorCostCalculator<TestTripSchedule> COST_CALCULATOR =
    new DefaultCostCalculator<>(20, 60, 1.0, null, null);

  public static final PathTailFilter<TestTripSchedule> FILTER_CHAIN =
    MinCostPathTailFilterFactory.ofCostFunction(OptimizedPathTail::generalizedCost);

  private final int STOP_TIME_ONE = TimeUtils.time("10:00");
  private final int STOP_TIME_TWO = TimeUtils.time("10:20");
  private final int STOP_TIME_THREE = TimeUtils.time("10:40");

  private final int STOP_POS_ONE = 0;
  private final int STOP_POS_TWO = 1;
  private final int STOP_POS_THREE = 2;

  private final OptimizedPathTail<TestTripSchedule> pathTail = new OptimizedPathTail<>(
    SLACK_PROVIDER,
    COST_CALCULATOR,
    0,
    null,
    null,
    0.0,
    this::stopIndexToName
  );

  private final TestTripSchedule TRIP = TestTripSchedule.schedule()
    .pattern("L1", STOP_A, STOP_C, STOP_E)
    .times(STOP_TIME_ONE, STOP_TIME_TWO, STOP_TIME_THREE)
    .build();

  private final int EGRESS_START = STOP_TIME_THREE + D1m;
  private final int EGRESS_END = EGRESS_START + D5m;

  @Test
  public void testEmptySetDoesReturnEmtySet() {
    var subject = new TransitPathLegSelector<>(FILTER_CHAIN, Set.of());
    assertEquals(Set.of(), subject.next(0));
  }

  @Test
  public void testOneElementIsReturnedIfTimeLimitThresholdIsPassed() {
    var leg = pathTail.addTransitTail(transitLeg(STOP_E));

    var subject = new TransitPathLegSelector<>(FILTER_CHAIN, Set.of(leg));

    var result = subject.next(STOP_POS_THREE);
    assertTrue(result.isEmpty(), result.toString());

    result = subject.next(STOP_POS_TWO);
    assertFalse(result.isEmpty(), result.toString());
  }

  @Test
  public void testTwoPathLegs() {
    var leg1 = pathTail.mutate().addTransitTail(transitLeg(STOP_E));
    var leg2 = pathTail.mutate().addTransitTail(transitLeg(STOP_C));

    var subject = new TransitPathLegSelector<>(FILTER_CHAIN, Set.of(leg1, leg2));

    var result = subject.next(STOP_POS_THREE);
    assertTrue(result.isEmpty(), result.toString());

    result = subject.next(STOP_POS_TWO);
    assertEquals("BUS L1 10:00 10:40", firstRide(result));
    assertEquals(result.size(), 1);

    result = subject.next(STOP_POS_ONE);
    assertEquals("BUS L1 10:00 10:20", firstRide(result));
    assertEquals(result.size(), 1);
  }

  private static <T> String firstRide(Collection<T> c) {
    return c
      .stream()
      .map(Object::toString)
      .map(it -> it.substring(0, it.indexOf(" ~")))
      .collect(Collectors.joining(" "));
  }

  private TransitPathLeg<TestTripSchedule> transitLeg(int egressStop) {
    var walk = TestAccessEgress.walk(egressStop, EGRESS_END - EGRESS_START);
    var egress = new EgressPathLeg<TestTripSchedule>(walk, EGRESS_START, EGRESS_END, walk.c1());
    int toTime = TRIP.arrival(TRIP.findArrivalStopPosition(Integer.MAX_VALUE, egressStop));
    int cost = 100 * (STOP_TIME_THREE - STOP_TIME_ONE);
    return new TransitPathLeg<>(
      TRIP,
      STOP_TIME_ONE,
      toTime,
      TRIP.findDepartureStopPosition(STOP_TIME_ONE, STOP_A),
      TRIP.findArrivalStopPosition(toTime, egressStop),
      null,
      cost,
      egress
    );
  }
}
