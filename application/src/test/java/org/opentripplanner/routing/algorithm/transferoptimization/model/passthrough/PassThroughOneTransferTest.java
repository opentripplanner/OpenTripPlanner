package org.opentripplanner.routing.algorithm.transferoptimization.model.passthrough;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.routing.algorithm.transferoptimization.model.passthrough.TestCase.testCase;
import static org.opentripplanner.routing.algorithm.transferoptimization.model.passthrough.TestUtils.domainService;
import static org.opentripplanner.routing.algorithm.transferoptimization.model.passthrough.TestUtils.pathBuilder;
import static org.opentripplanner.routing.algorithm.transferoptimization.model.passthrough.TestUtils.pathFocus;
import static org.opentripplanner.routing.algorithm.transferoptimization.model.passthrough.TestUtils.tx;
import static org.opentripplanner.utils.time.TimeUtils.time;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.raptorlegacy._data.RaptorTestConstants;
import org.opentripplanner.raptorlegacy._data.transit.TestTripSchedule;

/**
 * This test focus on the PASS-THROUGH functionality with two transit legs and one transfer in the
 * path. But between trip 1 and trip 2, there may be many transfer options to choose from.
 *
 * <p>
 * FEATURE UNDER TEST
 * <p>
 * We want the path with the lowest generalized-cost that visit the pass-through points in the
 * correct order.
 * <p>
 * TEST SETUP
 * <p>
 * We will use 2 trips with a fixed set of transfers for each test. Each trip has 5 stops and
 * plenty of slack to do the transfers for all possible stops combinations. We will set the
 * transfer durations to get different generalized-costs for each possible path. We will set the
 * cost so the transfers which do not contain any transfer-points have the lowest cost - is optimal
 * on generalized-cost. We do this to make sure the subject-under-test is using the pass-through-
 * points, and not the generalized cost to choose the correct path.
 */
@SuppressWarnings("SameParameterValue")
public class PassThroughOneTransferTest implements RaptorTestConstants {

  private static final int ITERATION_START_TIME = time("10:00");

  /**
   * We use arrays to store stuff per stop, so this is the max value of all stop indexes used,
   * plus one. Gaps are Ok, if they exist.
   */
  private static final int N_STOPS = STOP_M + 1;

  private final TestTripSchedule trip1 = TestTripSchedule.schedule()
    .pattern("Line 1", STOP_A, STOP_B, STOP_C, STOP_D, STOP_E)
    .times("10:05 10:10 10:15 10:20 10:25")
    .build();

  private final TestTripSchedule trip2 = TestTripSchedule.schedule()
    .pattern("Line 2", STOP_F, STOP_G, STOP_H, STOP_I, STOP_J)
    .times("10:30 10:35 10:40 10:45 10:50")
    .build();

  static List<TestCase> tripWithOneTransferTestCases() {
    return List.of(
      testCase().expectTransfer(STOP_C, STOP_H),
      testCase().points(STOP_B).expectTransfer(STOP_C, STOP_H),
      testCase().points(STOP_C).expectTransfer(STOP_D, STOP_H),
      testCase().points(STOP_D).expectTransfer(STOP_D, STOP_H),
      testCase().points(STOP_G).expectTransfer(STOP_C, STOP_G),
      testCase().points(STOP_H).expectTransfer(STOP_C, STOP_G),
      testCase().points(STOP_I).expectTransfer(STOP_C, STOP_H),
      // Two stops in one pass-through point
      testCase().points(STOP_B, STOP_C).expectTransfer(STOP_D, STOP_H),
      testCase().points(STOP_B, STOP_D).expectTransfer(STOP_C, STOP_H),
      testCase().points(STOP_B, STOP_G).expectTransfer(STOP_C, STOP_H),
      testCase().points(STOP_B, STOP_H).expectTransfer(STOP_C, STOP_G),
      testCase().points(STOP_B, STOP_I).expectTransfer(STOP_C, STOP_H),
      testCase().points(STOP_C, STOP_D).expectTransfer(STOP_C, STOP_H),
      testCase().points(STOP_C, STOP_G).expectTransfer(STOP_D, STOP_H),
      testCase().points(STOP_C, STOP_H).expectTransfer(STOP_D, STOP_G),
      testCase().points(STOP_C, STOP_I).expectTransfer(STOP_D, STOP_H),
      testCase().points(STOP_D, STOP_G).expectTransfer(STOP_C, STOP_G),
      testCase().points(STOP_D, STOP_H).expectTransfer(STOP_C, STOP_G),
      testCase().points(STOP_D, STOP_I).expectTransfer(STOP_C, STOP_H),
      testCase().points(STOP_G, STOP_H).expectTransfer(STOP_C, STOP_H),
      testCase().points(STOP_G, STOP_I).expectTransfer(STOP_C, STOP_H),
      testCase().points(STOP_H, STOP_I).expectTransfer(STOP_C, STOP_G),
      // Two stops in two pass-through points
      testCase().points(STOP_B).points(STOP_C).expectTransfer(STOP_D, STOP_H),
      testCase().points(STOP_B).points(STOP_D).expectTransfer(STOP_D, STOP_H),
      testCase().points(STOP_B).points(STOP_G).expectTransfer(STOP_C, STOP_G),
      testCase().points(STOP_B).points(STOP_H).expectTransfer(STOP_C, STOP_G),
      testCase().points(STOP_B).points(STOP_I).expectTransfer(STOP_C, STOP_H),
      testCase().points(STOP_C).points(STOP_D).expectTransfer(STOP_D, STOP_H),
      testCase().points(STOP_C).points(STOP_G).expectTransfer(STOP_D, STOP_G),
      testCase().points(STOP_C).points(STOP_H).expectTransfer(STOP_D, STOP_G),
      testCase().points(STOP_C).points(STOP_I).expectTransfer(STOP_D, STOP_H),
      testCase().points(STOP_D).points(STOP_G).expectTransfer(STOP_D, STOP_G),
      testCase().points(STOP_D).points(STOP_H).expectTransfer(STOP_D, STOP_G),
      testCase().points(STOP_D).points(STOP_I).expectTransfer(STOP_D, STOP_H),
      testCase().points(STOP_G).points(STOP_H).expectTransfer(STOP_C, STOP_G),
      testCase().points(STOP_G).points(STOP_I).expectTransfer(STOP_C, STOP_G),
      testCase().points(STOP_H).points(STOP_I).expectTransfer(STOP_C, STOP_G)
    );
  }

  /**
   * In this test we will use trip 1 and 2. We will have one test for each  possible pass-through-
   * point. We will add 4 transfers between the trips, [from]-[to]: {@code C-G, C-H, D-G, D-H}. We
   * will also add transfers between B-F and E-I, these transfers can not be used with the access
   * and egress, because we are not allowed to have two walking legs in a row. We include this
   * transfers to make sure the implementation ignores them.
   * <p>
   *
   * <pre>
   *           Origin
   *                \
   *  Trip 1   A --- B --- C --- D --- E
   *                 |     | \ / |     |
   *                 |     | / \ |     |
   *  Trip 2         F --- G --- H --- I --- J
   *                                    \
   *                                     Destination
   * </pre>
   * With this setup we will try all possible combinations of pass-through points and make sure
   * the correct path is chosen.
   * <p>
   * We will adjust the transfer walk duration so that paths containing the transfer-point get a
   * high cost, and paths without it get a lower cost.
   */
  @ParameterizedTest
  @MethodSource("tripWithOneTransferTestCases")
  public void tripWithOneTransfer(TestCase tc) {
    var txCost = new WalkDurationForStopCombinations(N_STOPS)
      .withPassThroughPoints(tc.points(), 10)
      // This transfer do not visit D
      .addTxCost(STOP_C, STOP_G, 2)
      // This transfer do not visit D and G; hence given the lowest cost
      .addTxCost(STOP_C, STOP_H, 1)
      // This transfer visit all stops; Hence given the highest cost
      .addTxCost(STOP_D, STOP_G, 3)
      // This transfer do not visit G
      .addTxCost(STOP_D, STOP_H, 2);

    // We need *a* path - the transfer here can be any.
    var originalPath = pathBuilder()
      .c2(tc.points().size())
      .access(ITERATION_START_TIME, STOP_B, D1s)
      .bus(trip1, STOP_D)
      .walk(txCost.walkDuration(STOP_D, STOP_F), STOP_F)
      .bus(trip2, STOP_I)
      .egress(D1s);

    var expectedPath = pathBuilder()
      .c2(tc.points().size())
      .access(ITERATION_START_TIME, STOP_B, D1s)
      .bus(trip1, tc.stopIndexA())
      .walk(txCost.walkDuration(tc.stopIndexA(), tc.stopIndexB()), tc.stopIndexB())
      .bus(trip2, STOP_I)
      .egress(D1s);

    // These are illegal transfers for the given path, we add them here to make sure they
    // do not interfere with the result. For simpler debugging problems try commenting out these
    // lines, just do not forget to comment them back in when the problem is fixed.
    var subject = domainService(
      tc.points(),
      List.of(
        tx(trip1, STOP_C, trip2, STOP_G, txCost),
        tx(trip1, STOP_C, trip2, STOP_H, txCost),
        tx(trip1, STOP_D, trip2, STOP_G, txCost),
        tx(trip1, STOP_D, trip2, STOP_H, txCost),
        // These are illegal transfers for the given path, we add them here to make sure they
        // do not interfere with the result. For simpler debugging problems try comment out these
        // lines, just do not forget to comment them back in when the problem is fixed.
        tx(trip1, STOP_B, trip2, STOP_F, txCost),
        tx(trip1, STOP_E, trip2, STOP_I, txCost)
      )
    );

    // When
    var result = subject.findBestTransitPath(originalPath);

    // Then expect a set containing the expected path only
    var resultAsString = result
      .stream()
      .map(it -> it.toString(this::stopIndexToName))
      .collect(Collectors.joining(", "));
    assertEquals(
      pathFocus(expectedPath.toString(this::stopIndexToName)),
      pathFocus(resultAsString),
      resultAsString
    );
  }
}
