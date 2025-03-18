package org.opentripplanner.routing.algorithm.transferoptimization.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.routing.algorithm.transferoptimization.services.TestTransferBuilder.tx;
import static org.opentripplanner.routing.algorithm.transferoptimization.services.TransferGeneratorDummy.dummyTransferGenerator;
import static org.opentripplanner.utils.time.TimeUtils.time;

import java.util.List;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.spi.DefaultSlackProvider;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.raptor.spi.RaptorSlackProvider;
import org.opentripplanner.raptorlegacy._data.RaptorTestConstants;
import org.opentripplanner.raptorlegacy._data.api.PathUtils;
import org.opentripplanner.raptorlegacy._data.api.TestPathBuilder;
import org.opentripplanner.raptorlegacy._data.transit.TestTripSchedule;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.DefaultCostCalculator;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TransferWaitTimeCostCalculator;
import org.opentripplanner.routing.algorithm.transferoptimization.model.costfilter.MinCostPathTailFilterFactory;

public class OptimizePathDomainServiceTest implements RaptorTestConstants {

  /**
   * The exact start time to walk to stop A to catch Trip_1 with 40s board slack
   */
  private static final int ITERATION_START_TIME = time("10:00");
  private static final int TRANSFER_SLACK = D1m;
  private static final int BOARD_SLACK = D40s;
  private static final int ALIGHT_SLACK = D20s;
  private static final int BOARD_COST_SEC = 10;
  private static final int TRANSFER_COST_SEC = 20;
  private static final double WAIT_RELUCTANCE = 1.0;

  private static final RaptorSlackProvider SLACK_PROVIDER = new DefaultSlackProvider(
    TRANSFER_SLACK,
    BOARD_SLACK,
    ALIGHT_SLACK
  );

  public static final RaptorCostCalculator<TestTripSchedule> COST_CALCULATOR =
    new DefaultCostCalculator<>(BOARD_COST_SEC, TRANSFER_COST_SEC, WAIT_RELUCTANCE, null, null);

  private static final TransferWaitTimeCostCalculator TRANS_WAIT_TIME_CALC =
    new TransferWaitTimeCostCalculator(1.0, 2.0);

  static {
    TRANS_WAIT_TIME_CALC.setMinSafeTransferTime(D5m);
  }

  /**
   * A Path without any transfers should be returned without any change.
   */
  @Test
  public void testTripWithoutTransfers() {
    // Given a trip A-B-C-D
    var trip1 = TestTripSchedule.schedule()
      .pattern("T1", STOP_A, STOP_B, STOP_C, STOP_D)
      .times("10:02 10:10 10:20 10:30")
      .build();

    // Use only in-same-stop transfers
    var transfers = dummyTransferGenerator();

    // and a path: Walk ~ B ~ T1 ~ C ~ Walk
    var original = pathBuilder()
      .access(ITERATION_START_TIME, STOP_B, D1m)
      .bus(trip1, STOP_C)
      .c2(345)
      .egress(D1m);

    var subject = subject(transfers, null);

    // When
    var result = subject.findBestTransitPath(original);

    // Then expect a set containing the original path
    assertEquals(
      original.toStringDetailed(this::stopIndexToName),
      PathUtils.pathsToStringDetailed(result)
    );
  }

  /**
   * This test emulates the normal case were there is only one option to transfer between two trips
   * and we should find the exact same option. The path should exactly match the original path after
   * the path is reconstructed.
   */
  @Test
  public void testTripWithOneTransfer() {
    // Given
    var trip1 = TestTripSchedule.schedule()
      .arrDepOffset(D0s)
      .pattern("T1", STOP_A, STOP_B, STOP_C, STOP_D)
      .times("10:02 10:10 10:20 10:30")
      .build();

    var trip2 = TestTripSchedule.schedule()
      .arrDepOffset(D0s)
      .pattern("T2", STOP_E, STOP_F, STOP_G)
      .times("10:12 10:22 10:50")
      .build();

    var transfers = dummyTransferGenerator(
      List.of(tx(trip1, STOP_C, trip2, STOP_F).walk(D30s).build())
    );

    // Path:  Access ~ B ~ T1 ~ C ~ Walk 30s ~ D ~ T2 ~ E ~ Egress
    var original = pathBuilder()
      .access(ITERATION_START_TIME, STOP_B, D1m)
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
      .replace("C₁2_770]", "C₁2_770 Tₚ3_300 wtC₁3_103.81]");

    assertEquals(expected, PathUtils.pathsToStringDetailed(result));
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
    var trip1 = TestTripSchedule.schedule()
      .pattern("T1", STOP_A, STOP_B, STOP_D)
      .times("10:02 10:10 10:20")
      .build();

    var trip2 = TestTripSchedule.schedule()
      .pattern("T2", STOP_B, STOP_C, STOP_D, STOP_F)
      .times("10:12 10:15 10:22 10:35")
      .build();

    var trip3 = TestTripSchedule.schedule()
      .pattern("T3", STOP_E, STOP_F, STOP_G)
      .times("10:24 10:37 10:49")
      .build();

    var transfers = dummyTransferGenerator(
      List.of(
        tx(trip1, STOP_B, trip2).build(),
        tx(trip1, STOP_B, trip2, STOP_C).walk(D30s).build(),
        tx(trip1, STOP_D, trip2).build()
      ),
      List.of(tx(trip2, STOP_D, trip3, STOP_E).walk(D30s).build(), tx(trip2, STOP_F, trip3).build())
    );

    var original = pathBuilder()
      .access(ITERATION_START_TIME, STOP_A)
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
      "A ~ BUS T1 10:02 10:10 ~ B ~ BUS T2 10:12 10:35 ~ F ~ BUS T3 10:37 10:49 ~ G " +
      "[10:01:20 10:49:20 48m Tₓ2 C₁2_950 Tₚ6_600]",
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
      "A ~ BUS T1 10:02 10:10 ~ B ~ Walk 30s ~ C " +
      "~ BUS T2 10:15 10:35 ~ F " +
      "~ BUS T3 10:37 10:49 ~ G " +
      "[10:01:20 10:49:20 48m Tₓ2 C₁2_980 Tₚ6_600 wtC₁3_294.05]",
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
    var trip1 = TestTripSchedule.schedule()
      .pattern("T1", STOP_A, STOP_B, STOP_C)
      .times("10:02 10:10 10:15")
      .build();

    var trip2 = TestTripSchedule.schedule()
      .pattern("T2", STOP_B, STOP_C, STOP_D)
      .times("10:13 10:17 10:30")
      .build();

    var transfers = dummyTransferGenerator(
      List.of(
        tx(trip1, STOP_B, trip2).build(),
        tx(trip1, STOP_C, trip2, STOP_C).guaranteed().build()
      )
    );

    var original = pathBuilder()
      .access(ITERATION_START_TIME, STOP_A)
      .bus(trip1, STOP_B)
      .bus(trip2, STOP_D)
      .egress(D0s);

    var subject = subject(transfers, null);

    // Find the path with the lowest cost
    var result = subject.findBestTransitPath(original);

    assertEquals(1, result.size(), result.toString());

    var it = result.iterator().next();

    assertEquals(
      "A ~ BUS T1 10:02 10:15 ~ C ~ BUS T2 10:17 10:30 ~ D [10:01:20 10:30:20 29m Tₓ1 C₁1_750 Tₚ2_300]",
      it.toString(this::stopIndexToName)
    );
    // Verify the attached Transfer is exist and is valid
    assertEquals(
      "ConstrainedTransfer{from: TripTP{F:BUS T1:10:02, stopPos 2}, to: TripTP{F:BUS T2:10:13, stopPos 1}, constraint: {guaranteed}}",
      it.accessLeg().nextLeg().asTransitLeg().getConstrainedTransferAfterLeg().toString()
    );
  }

  /**
   * <pre>
   * DEPARTURE TIMES
   * Stop        A      B      C      D
   * Trip 1    10:10  10:10  10:15
   * Trip 2           10:13  10:13  10:30
   * </pre>
   * Case: A trip may have the exact same times for more than one stop. This is a regression test
   *       see https://github.com/opentripplanner/OpenTripPlanner/issues/5444.
   *       The following transfers exist: A-B, A-C, B-B, B-C, C-B and C-C.
   * Expect: Transfer B-B, the earliest transfer with the lowest transfer time and cost.
   */
  @Test
  public void testSameStopTimesInPattern() {
    // Given
    var trip1 = TestTripSchedule.schedule()
      .pattern("T1", STOP_A, STOP_B, STOP_C)
      .times("10:10 10:10 10:15")
      .build();

    var trip2 = TestTripSchedule.schedule()
      .pattern("T2", STOP_B, STOP_C, STOP_D)
      .times("10:13 10:13 10:30")
      .build();

    var transfers = dummyTransferGenerator(
      List.of(
        tx(trip1, STOP_A, trip2, STOP_B).walk(D10s).build(),
        tx(trip1, STOP_A, trip2, STOP_C).walk(D10s).build(),
        tx(trip1, STOP_B, trip2).build(),
        tx(trip1, STOP_B, trip2, STOP_C).walk(D10s).build(),
        tx(trip1, STOP_C, trip2, STOP_B).walk(D10s).build(),
        tx(trip1, STOP_C, trip2).build()
      )
    );

    var original = pathBuilder()
      .access(ITERATION_START_TIME, STOP_A)
      .bus(trip1, STOP_B)
      .bus(trip2, STOP_D)
      .egress(D0s);

    var subject = subject(transfers, null);

    // Find the path with the lowest cost
    var result = subject.findBestTransitPath(original);

    assertEquals(
      "A ~ BUS T1 10:10 10:10 ~ B ~ BUS T2 10:13 10:30 ~ D [10:09:20 10:30:20 21m Tₓ1 C₁1_300 Tₚ3_300]",
      PathUtils.pathsToString(result)
    );
  }

  static TestPathBuilder pathBuilder() {
    return new TestPathBuilder(
      new DefaultSlackProvider(TRANSFER_SLACK, BOARD_SLACK, ALIGHT_SLACK),
      COST_CALCULATOR
    );
  }

  /* private methods */

  static OptimizePathDomainService<TestTripSchedule> subject(
    TransferGenerator<TestTripSchedule> generator,
    @Nullable TransferWaitTimeCostCalculator waitTimeCalculator
  ) {
    var filter = new MinCostPathTailFilterFactory<TestTripSchedule>(
      true,
      waitTimeCalculator != null
    ).createFilter();
    return new OptimizePathDomainService<>(
      generator,
      COST_CALCULATOR,
      SLACK_PROVIDER,
      waitTimeCalculator,
      null,
      0.0,
      filter,
      (new RaptorTestConstants() {})::stopIndexToName
    );
  }
}
