package org.opentripplanner.routing.algorithm.transferoptimization.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.framework.time.TimeUtils.time;
import static org.opentripplanner.routing.algorithm.transferoptimization.services.TestTransferBuilder.txConstrained;
import static org.opentripplanner.routing.algorithm.transferoptimization.services.TransferGeneratorDummy.dummyTransferGenerator;
import static org.opentripplanner.routing.algorithm.transferoptimization.services.TransferGeneratorDummy.tx;

import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.api.PathUtils;
import org.opentripplanner.raptor._data.api.TestPathBuilder;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.spi.CostCalculator;
import org.opentripplanner.raptor.spi.RaptorSlackProvider;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.DefaultCostCalculator;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TransferWaitTimeCostCalculator;

public class OptimizePathDomainServiceTest implements RaptorTestConstants {

  /**
   * The exact start time to walk to stop A to catch Trip_1 with 40s board slack
   */
  private static final int START_TIME_T1 = time("10:00:20");
  private static final int TRANSFER_SLACK = D1m;
  private static final int BOARD_SLACK = D40s;
  private static final int ALIGHT_SLACK = D20s;
  private static final int BOARD_COST_SEC = 10;
  private static final int TRANSFER_COST_SEC = 20;
  private static final double WAIT_RELUCTANCE = 1.0;

  private static final RaptorSlackProvider SLACK_PROVIDER = RaptorSlackProvider.defaultSlackProvider(
    TRANSFER_SLACK,
    BOARD_SLACK,
    ALIGHT_SLACK
  );

  public static final CostCalculator<TestTripSchedule> COST_CALCULATOR = new DefaultCostCalculator<>(
    BOARD_COST_SEC,
    TRANSFER_COST_SEC,
    WAIT_RELUCTANCE,
    null,
    null
  );

  private static final TransferWaitTimeCostCalculator TRANS_WAIT_TIME_CALC = new TransferWaitTimeCostCalculator(
    1.0,
    2.0
  );

  static {
    TRANS_WAIT_TIME_CALC.setMinSafeTransferTime(D5m);
  }

  /**
   * A Path without any transfers should be returned without any change.
   */
  @Test
  public void testTripWithoutTransfers() {
    // Given a trip A-B-C-D
    var trip1 = TestTripSchedule
      .schedule()
      .pattern("T1", STOP_A, STOP_B, STOP_C, STOP_D)
      .times("10:02 10:10 10:20 10:30")
      .build();

    // Use only in-same-stop transfers
    var transfers = dummyTransferGenerator();

    // and a path: Walk ~ B ~ T1 ~ C ~ Walk
    var original = pathBuilder().access(START_TIME_T1, D1m, STOP_B).bus(trip1, STOP_C).egress(D1m);

    var subject = subject(transfers, null);

    // When
    var result = subject.findBestTransitPath(original);

    // Then expect a set containing the original path
    assertEquals(
      original.toStringDetailed(this::stopIndexToName),
      first(result).toStringDetailed(this::stopIndexToName)
    );
    assertEquals(1, result.size());
  }

  /**
   * This test emulates the normal case were there is only one option to transfer between two trips
   * and we should find the exact same option. The path should exactly match the original path after
   * the path is reconstructed.
   */
  @Test
  public void testTripWithOneTransfer() {
    // Given
    var trip1 = TestTripSchedule
      .schedule()
      .arrDepOffset(D0s)
      .pattern("T1", STOP_A, STOP_B, STOP_C, STOP_D)
      .times("10:02 10:10 10:20 10:30")
      .build();

    var trip2 = TestTripSchedule
      .schedule()
      .arrDepOffset(D0s)
      .pattern("T2", STOP_E, STOP_F, STOP_G)
      .times("10:12 10:22 10:50")
      .build();

    var transfers = dummyTransferGenerator(List.of(tx(trip1, STOP_C, D30s, STOP_F, trip2)));

    // Path:  Access ~ B ~ T1 ~ C ~ Walk 30s ~ D ~ T2 ~ E ~ Egress
    var original = pathBuilder()
      .access(START_TIME_T1, D1m, STOP_B)
      .bus(trip1, STOP_C)
      .walk(D30s, STOP_F)
      .bus(trip2, STOP_G)
      .egress(D1m);

    var subject = subject(transfers, TRANS_WAIT_TIME_CALC);

    // When
    var result = subject.findBestTransitPath(original);

    // Insert wait-time cost summary info
    var expected = original
      .toStringDetailed(this::stopIndexToName)
      .replace("$3250]", "$3250 $33pri $3583.81wtc]");

    assertEquals(expected, first(result).toStringDetailed(this::stopIndexToName));
    assertEquals(1, result.size());
  }

  /**
   * <pre>
   * DEPARTURE TIMES
   * Stop        A      B      C      D      E      F      G
   * Trip 1    10:02  10:10         10:20
   * Trip 2           10:12  10:15  10:22         10:35
   * Trip 3                                10:24  10:37  10:49
   * </pre>
   */
  @Test
  public void testPathWithThreeTripsAndMultiplePlacesToTransfer() {
    // Given
    var trip1 = TestTripSchedule
      .schedule()
      .pattern("T1", STOP_A, STOP_B, STOP_D)
      .times("10:02 10:10 10:20")
      .build();

    var trip2 = TestTripSchedule
      .schedule()
      .pattern("T2", STOP_B, STOP_C, STOP_D, STOP_F)
      .times("10:12 10:15 10:22 10:35")
      .build();

    var trip3 = TestTripSchedule
      .schedule()
      .pattern("T3", STOP_E, STOP_F, STOP_G)
      .times("10:24 10:37 10:49")
      .build();

    var transfers = dummyTransferGenerator(
      List.of(
        tx(trip1, STOP_B, trip2),
        tx(trip1, STOP_B, D30s, STOP_C, trip2),
        tx(trip1, STOP_D, trip2)
      ),
      List.of(tx(trip2, STOP_D, D30s, STOP_E, trip3), tx(trip2, STOP_F, trip3))
    );

    var original = pathBuilder()
      .access(START_TIME_T1, D0s, STOP_A)
      .bus(trip1, STOP_B)
      .bus(trip2, STOP_D)
      .walk(D30s, STOP_E)
      .bus(trip3, STOP_G)
      .egress(D0s);

    // First we do the test without a wait-time cost calculator, which should pick the
    // option with the lowest cost and as early as possible. So the preferred transfer
    // between Trip 1 and 2 is at stop B - no walking (B to C) and before D. The preferred
    // transfer between Trip 2 and 3 is at stop F.
    var subject = subject(transfers, null);

    // Find the path with the lowest cost
    var result = subject.findBestTransitPath(original);

    assertEquals(
      "A ~ BUS T1 10:02 10:10 ~ B ~ BUS T2 10:12 10:35 ~ F ~ " +
      "BUS T3 10:37 10:49 ~ G [10:00:20 10:49:20 49m 2tx $3010 $66pri]",
      PathUtils.pathsToString(result)
    );

    // Then we do the test with the wait-time cost calculator, which should pick the
    // option with the lowest wait-time cost. For the first transfer, the transfer between
    // stop B and C with a 30 sec walk is the one maximising the wait time. The preferred
    // transfer between Trip 2 and 3 is at stop F (same as case 1).
    subject = subject(transfers, TRANS_WAIT_TIME_CALC);

    // Find the path with the lowest cost
    result = subject.findBestTransitPath(original);

    assertEquals(
      "A ~ BUS T1 10:02 10:10 ~ B ~ Walk 30s ~ C ~ BUS T2 10:15 10:35 ~ F " +
      "~ BUS T3 10:37 10:49 ~ G [10:00:20 10:49:20 49m 2tx $3040 $66pri $3354.05wtc]",
      PathUtils.pathsToString(result)
    );
  }

  /**
   * <pre>
   * DEPARTURE TIMES
   * Stop        A      B      C      D
   * Trip 1    10:02  10:10  10:15
   * Trip 2           10:13  10:17  10:30
   * </pre>
   * Case: Transfer at stop B is returned, but transfer at stop C i guaranteed
   * Expect: Transfer at C and transfer info attached
   */
  @Test
  public void testConstrainedTransferIsPreferred() {
    // Given
    var trip1 = TestTripSchedule
      .schedule()
      .pattern("T1", STOP_A, STOP_B, STOP_C)
      .times("10:02 10:10 10:15")
      .build();

    var trip2 = TestTripSchedule
      .schedule()
      .pattern("T2", STOP_B, STOP_C, STOP_D)
      .times("10:13 10:17 10:30")
      .build();

    var transfers = dummyTransferGenerator(
      List.of(
        tx(trip1, STOP_B, trip2),
        tx(txConstrained(trip1, STOP_C, trip2, STOP_C).guaranteed())
      )
    );

    var original = pathBuilder()
      .access(START_TIME_T1, 0, STOP_A)
      .bus(trip1, STOP_B)
      .bus(trip2, STOP_D)
      .egress(D0s);

    var subject = subject(transfers, null);

    // Find the path with the lowest cost
    var result = subject.findBestTransitPath(original);

    assertEquals(1, result.size(), result.toString());

    var it = result.iterator().next();

    assertEquals(
      "A ~ BUS T1 10:02 10:15 ~ C ~ BUS T2 10:17 10:30 ~ D [10:00:20 10:30:20 30m 1tx $1810 $23pri]",
      it.toString(this::stopIndexToName)
    );
    // Verify the attached Transfer is exist and is valid
    assertEquals(
      "ConstrainedTransfer{from: TripTP{F:BUS T1:10:02, stopPos 2}, to: TripTP{F:BUS T2:10:13, stopPos 1}, constraint: {guaranteed}}",
      it.accessLeg().nextLeg().asTransitLeg().getConstrainedTransferAfterLeg().toString()
    );
  }

  static TestPathBuilder pathBuilder() {
    return new TestPathBuilder(ALIGHT_SLACK, COST_CALCULATOR);
  }

  /* private methods */

  static OptimizePathDomainService<TestTripSchedule> subject(
    TransferGenerator<TestTripSchedule> generator,
    @Nullable TransferWaitTimeCostCalculator waitTimeCalculator
  ) {
    return new OptimizePathDomainService<>(
      generator,
      COST_CALCULATOR,
      SLACK_PROVIDER,
      waitTimeCalculator,
      null,
      0.0,
      TransferOptimizedFilterFactory.filter(true, waitTimeCalculator != null),
      (new RaptorTestConstants() {})::stopIndexToName
    );
  }

  static <T> T first(Collection<T> c) {
    return c.stream().findFirst().orElseThrow();
  }
}
