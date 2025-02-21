package org.opentripplanner.routing.algorithm.transferoptimization.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.api.path.TransitPathLeg;
import org.opentripplanner.raptorlegacy._data.RaptorTestConstants;
import org.opentripplanner.raptorlegacy._data.transit.TestTripSchedule;
import org.opentripplanner.routing.algorithm.transferoptimization.BasicPathTestCase;
import org.opentripplanner.routing.algorithm.transferoptimization.services.TestTransferBuilder;

class OptimizedPathTailTest implements RaptorTestConstants {

  private final RaptorPath<TestTripSchedule> orgPath = BasicPathTestCase.basicTripAsPath();

  private final RaptorPath<TestTripSchedule> flexPath = BasicPathTestCase.flexTripAsPath();

  private final TransitPathLeg<TestTripSchedule> t1 = orgPath.accessLeg().nextTransitLeg();

  @SuppressWarnings("ConstantConditions")
  private final TransitPathLeg<TestTripSchedule> t2 = t1.nextTransitLeg();

  @SuppressWarnings("ConstantConditions")
  private final TransitPathLeg<TestTripSchedule> t3 = t2.nextTransitLeg();

  @SuppressWarnings("ConstantConditions")
  private final TripToTripTransfer<TestTripSchedule> tx23 = TestTransferBuilder
    .tx(t2.trip(), STOP_D, t3.trip(), STOP_D)
    .staySeated()
    .build();

  private final TripToTripTransfer<TestTripSchedule> tx12 = TestTransferBuilder
    .tx(t1.trip(), STOP_B, t2.trip(), STOP_C)
    .walk(D2m)
    .build();

  private final TransferWaitTimeCostCalculator waitTimeCalc = new TransferWaitTimeCostCalculator(
    1.0,
    5.0
  );

  /**
   * Give stop B and D an extra stop-priority-cost.
   * <p>
   * Stop B is visited once and stop D twice. This will add the following extra cost to the path:
   * <pre>
   *  - extraStopBoardAlightCostsFactor = 2.0
   *  - Cost stop B = 30s ($30.00)
   *  - Cost stop D = 10s ($10.00)
   *
   *  Extra cost = (30.0 + 2 * 10.0) * 2.0 = $100.00
   *  </pre>
   */
  private final int[] stopBoardAlightTransferCosts = new int[] { 0, 0, 3000, 0, 1000, 0 };

  private final OptimizedPathTail<TestTripSchedule> subject = new OptimizedPathTail<>(
    SLACK_PROVIDER,
    BasicPathTestCase.C1_CALCULATOR,
    0,
    waitTimeCalc,
    stopBoardAlightTransferCosts,
    2.0,
    this::stopIndexToName
  );

  @BeforeEach
  void setup() {
    waitTimeCalc.setMinSafeTransferTime(D5m);
  }

  @Test
  void testToString() {
    subject.addTransitTail(t3);
    subject.addTransitAndTransferLeg(t2, tx23);
    subject.addTransitAndTransferLeg(t1, tx12);
    subject.access(orgPath.accessLeg().access());

    var exp =
      "Walk 3m ~ A " +
      "~ BUS L11 10:04 10:35 ~ B " +
      "~ Walk 2m ~ C " +
      "~ BUS L21 11:00 11:23 ~ D " +
      "~ BUS L31 11:40 11:52 ~ E " +
      "~ Walk 7m45s " +
      "[C₁7_989 Tₚ4_600 wtC₁-93_137]";

    assertEquals(exp, subject.toString());
  }

  @Test
  void shouldHandleATransferAfterLastTransit() {
    subject.addTransitTail(flexPath.accessLeg().nextTransitLeg());
    subject.access(orgPath.accessLeg().access());

    var exp =
      "Walk 3m ~ A " +
      "~ BUS L11 10:04 10:35 ~ B " +
      "~ Walk 3m45s ~ E " +
      "~ Flex 7m45s 1x " +
      "[C₁3_906 wtC₁3_966]";

    assertEquals(exp, subject.toString());
  }

  @Test
  void testMutate() {
    OptimizedPathTail<TestTripSchedule> copy;

    subject.addTransitTail(t3);

    copy = subject.mutate();
    assertEquals(subject.toString(), copy.toString());

    subject.addTransitAndTransferLeg(t2, tx23);

    copy = subject.mutate();
    assertEquals(subject.toString(), copy.toString());

    subject.addTransitAndTransferLeg(t1, tx12);

    copy = subject.mutate();
    assertEquals(subject.toString(), copy.toString());

    subject.access(orgPath.accessLeg().access());

    copy = subject.mutate();
    assertEquals(subject.toString(), copy.toString());
  }

  @Test
  void testBuildingPath() {
    subject.addTransitTail(t3);
    subject.addTransitAndTransferLeg(t2, tx23);
    subject.addTransitAndTransferLeg(t1, tx12);
    subject.access(orgPath.accessLeg().access());

    var path = subject.build();

    // We have replaced the first transfer with a 2 minute walk
    var expPath =
      "Walk 3m 10:00:15 10:03:15 C₁360 ~ A 45s " +
      "~ BUS L11 10:04 10:35 31m C₁1_998 ~ B 15s " +
      "~ Walk 2m 10:35:15 10:37:15 C₁240 ~ C 22m45s " +
      "~ BUS L21 11:00 11:23 23m C₁2_724 ~ D 17m {staySeated} " +
      "~ BUS L31 11:40 11:52 12m C₁1_737 ~ E 15s " +
      "~ Walk 7m45s 11:52:15 12:00 C₁930 " +
      "[10:00:15 12:00 1h59m45s Tₓ1 C₁7_989 Tₚ4_600 wtC₁-93_137]";

    assertEquals(expPath, path.toStringDetailed(this::stopIndexToName));
  }
}
