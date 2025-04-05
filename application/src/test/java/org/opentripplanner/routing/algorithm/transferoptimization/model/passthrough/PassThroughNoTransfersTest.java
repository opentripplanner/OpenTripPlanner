package org.opentripplanner.routing.algorithm.transferoptimization.model.passthrough;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.routing.algorithm.transferoptimization.model.passthrough.TestCase.testCase;
import static org.opentripplanner.routing.algorithm.transferoptimization.model.passthrough.TestUtils.domainService;
import static org.opentripplanner.routing.algorithm.transferoptimization.model.passthrough.TestUtils.first;
import static org.opentripplanner.routing.algorithm.transferoptimization.model.passthrough.TestUtils.pathBuilder;
import static org.opentripplanner.utils.time.TimeUtils.time;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.raptorlegacy._data.RaptorTestConstants;
import org.opentripplanner.raptorlegacy._data.transit.TestTripSchedule;

/**
 * This test focus on the PASS-THROUGH functionality with a very simple scenario - one transit leg
 * and no transfers.
 * <p>
 * FEATURE UNDER TEST
 * <p>
 * We want the path with the lowest generalized-cost that visits the pass-through points in the
 * correct order.
 * <p>
 * TEST SETUP
 * <p>
 * We will use one trip with 5 stops. There is only one possible path - the original.
 */
@SuppressWarnings("SameParameterValue")
public class PassThroughNoTransfersTest implements RaptorTestConstants {

  private static final int ITERATION_START_TIME = time("10:00");
  /** Any stop not part of trip. */
  private static final int ANY_STOP = STOP_I;

  private final TestTripSchedule trip1 = TestTripSchedule.schedule()
    .pattern("Line 1", STOP_A, STOP_B, STOP_C, STOP_D, STOP_E)
    .times("10:05 10:10 10:15 10:20 10:25")
    .build();

  static List<TestCase> tripWithoutTransfersTestCases() {
    return List.of(
      testCase("").build(),
      testCase("at board stop B").points(STOP_B).build(),
      testCase("at intermediate stop C").points(STOP_C).build(),
      testCase("at alight stop D").points(STOP_D).build(),
      testCase("at either B or C").points(STOP_B, STOP_C).build(),
      testCase("at either C or D").points(STOP_C, STOP_D).build(),
      testCase("at C, w/unreachable A").points(STOP_A, STOP_C).build(),
      testCase("at C, w/unreachable E").points(STOP_C, STOP_E).build(),
      testCase("at C, w/unreachable stop not part of trip").points(STOP_C, ANY_STOP).build(),
      testCase("at board stop B & intermediate stop C").points(STOP_B).points(STOP_C).build(),
      testCase("at intermediate stop C & alight stop D").points(STOP_C).points(STOP_D).build(),
      testCase("at board stop C & the alight stop D").points(STOP_C).points(STOP_D).build(),
      testCase("at B, C, and D")
        .points(STOP_A, STOP_B)
        .points(STOP_C, STOP_I)
        .points(STOP_D)
        .build()
    );
  }

  /**
   * The pass-through point used can be the board-, intermediate-, and/or alight-stop. We will also
   * test all combinations of these to make sure a pass-through point is only accounted for once.
   * <p>
   * The trip1 used:
   * <pre>
   * Origin ~ walk ~ B ~ Trip 1 ~ D ~ walk ~ Destination
   * </pre>
   * Note! Stop A and E is not visited. Stop I is part of one transfer-point, but not part of the
   * trip.
   */
  @ParameterizedTest
  @MethodSource("tripWithoutTransfersTestCases")
  public void tripWithoutTransfers(TestCase tc) {
    var originalPath = pathBuilder()
      .c2(tc.points().size())
      .access(ITERATION_START_TIME, STOP_B, D1s)
      .bus(trip1, STOP_D)
      .egress(D1s);

    var subject = domainService(tc.points());

    // When
    var result = subject.findBestTransitPath(originalPath);

    assertEquals(1, result.size());

    // Then expect a set containing the original path
    assertEquals(
      originalPath.toString(this::stopIndexToName),
      first(result).toString(this::stopIndexToName)
    );
  }
}
