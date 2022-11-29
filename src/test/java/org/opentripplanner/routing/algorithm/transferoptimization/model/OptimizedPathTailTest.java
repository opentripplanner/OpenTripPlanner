package org.opentripplanner.routing.algorithm.transferoptimization.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.routing.algorithm.transferoptimization.services.TestTransferBuilder.txConstrained;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.stoparrival.BasicPathTestCase;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.path.Path;
import org.opentripplanner.raptor.api.path.TransitPathLeg;
import org.opentripplanner.routing.algorithm.transferoptimization.services.TransferGeneratorDummy;

class OptimizedPathTailTest implements RaptorTestConstants {

  private final Path<TestTripSchedule> orgPath = BasicPathTestCase.basicTripAsPath();

  private final Path<TestTripSchedule> flexPath = BasicPathTestCase.flexTripAsPath();

  private final TransitPathLeg<TestTripSchedule> t1 = orgPath.accessLeg().nextTransitLeg();

  @SuppressWarnings("ConstantConditions")
  private final TransitPathLeg<TestTripSchedule> t2 = t1.nextTransitLeg();

  @SuppressWarnings("ConstantConditions")
  private final TransitPathLeg<TestTripSchedule> t3 = t2.nextTransitLeg();

  @SuppressWarnings("ConstantConditions")
  private final TripToTripTransfer<TestTripSchedule> tx23 = TransferGeneratorDummy.tx(
    txConstrained(t2.trip(), STOP_D, t3.trip(), STOP_D).staySeated()
  );

  private final TripToTripTransfer<TestTripSchedule> tx12 = TransferGeneratorDummy.tx(
    t1.trip(),
    STOP_B,
    D2m,
    STOP_C,
    t2.trip()
  );
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
  private final int[] stopBoardAlightCost = new int[] { 0, 0, 3000, 0, 1000, 0 };

  private final OptimizedPathTail<TestTripSchedule> subject = new OptimizedPathTail<>(
    BasicPathTestCase.SLACK_PROVIDER,
    BasicPathTestCase.COST_CALCULATOR,
    waitTimeCalc,
    stopBoardAlightCost,
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
      "Walk 3m15s ~ A " +
      "~ BUS L11 10:04 10:35 ~ B " +
      "~ Walk 2m ~ C " +
      "~ BUS L21 11:00 11:23 ~ D " +
      "~ BUS L31 11:40 11:52 ~ E " +
      "~ Walk 7m45s " +
      "[$8019 $46pri $-93107wtc]";

    assertEquals(exp, subject.toString());
  }

  @Test
  void shouldHandleATransferAfterLastTransit() {
    subject.addTransitTail(flexPath.accessLeg().nextTransitLeg());
    subject.access(orgPath.accessLeg().access());

    var exp =
      "Walk 3m15s ~ A " +
      "~ BUS L11 10:04 10:35 ~ B " +
      "~ Walk 3m45s ~ E " +
      "~ Flex 7m45s 1x " +
      "[$3936 $0pri $3996wtc]";

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

    var path = subject.build(0);

    // We have replaced the first transfer with a 2 minute walk
    var expPath =
      "Walk 3m15s 10:00 10:03:15 $390 ~ A 45s " +
      "~ BUS L11 10:04 10:35 31m $1998 ~ B 15s " +
      "~ Walk 2m 10:35:15 10:37:15 $240 ~ C 22m45s " +
      "~ BUS L21 11:00 11:23 23m $2724 ~ D 17m {staySeated} " +
      "~ BUS L31 11:40 11:52 12m $1737 ~ E 15s " +
      "~ Walk 7m45s 11:52:15 12:00 $930 " +
      "[10:00 12:00 2h 1tx $8019 $46pri $-93107wtc]";

    assertEquals(expPath, path.toStringDetailed(this::stopIndexToName));
  }
}
