package org.opentripplanner.routing.algorithm.transferoptimization.model.passthrough;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.routing.algorithm.transferoptimization.model.passthrough.TestCase.testCase;
import static org.opentripplanner.routing.algorithm.transferoptimization.model.passthrough.TestUtils.domainService;
import static org.opentripplanner.routing.algorithm.transferoptimization.model.passthrough.TestUtils.pathBuilder;
import static org.opentripplanner.routing.algorithm.transferoptimization.model.passthrough.TestUtils.tx;
import static org.opentripplanner.utils.time.TimeUtils.time;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptorlegacy._data.RaptorTestConstants;
import org.opentripplanner.raptorlegacy._data.api.PathUtils;
import org.opentripplanner.raptorlegacy._data.transit.TestTripSchedule;

/**
 *  This test focus on the PASS-THROUGH functionality with three transit legs and two transfer in
 *  the path.
 *  <p>
 *  FEATURE UNDER TEST
 *  <p>
 *  We want the path with the lowest generalized-cost that visit the pass-through points in the
 *  correct order.
 *  <p>
 *  TEST SETUP
 *  <p>
 *  We will use 3 trips with a fixed set of transfers for each test. Each trip has 5 stops and
 *  plenty of slack to do the transfers for all possible stops combinations. There is two
 *  transfers to choose from between trip 1 and trip 2, and between trip 2 and trip 3. We will set
 *  the transfer durations to get different generalized-costs for each possible path. We will set
 *  the cost so the transfers which do not contain any transfer-points have the lowest cost - is
 *  optimal on generalized-cost. We do this to make sure the subject-under-test is using the
 *  pass-through-points, and not the generalized cost to choose the correct path.
 */
@SuppressWarnings("SameParameterValue")
public class PassThroughTwoTransfersTest implements RaptorTestConstants {

  private static final int ITERATION_START_TIME = time("10:00");

  private final TestTripSchedule trip1 = TestTripSchedule.schedule()
    .pattern("Line 1", STOP_A, STOP_B, STOP_C, STOP_D, STOP_E)
    .times("10:05 10:10 10:15 10:20 10:25")
    .build();

  private final TestTripSchedule trip2 = TestTripSchedule.schedule()
    .pattern("Line 2", STOP_F, STOP_G, STOP_H, STOP_I, STOP_J)
    .times("10:30 10:35 10:40 10:45 10:50")
    .build();

  private final TestTripSchedule trip3 = TestTripSchedule.schedule()
    .pattern("Line 3", STOP_K, STOP_L, STOP_M)
    .times("10:55 11:00 11:05")
    .build();

  static List<TestCase> tripWithTwoTransferTestCases() {
    return List.of(
      // None pass-through-points
      testCase("").expectTransfersFrom(STOP_C, STOP_H),
      // One pass-through-points
      testCase().points(STOP_B).expectTransfersFrom(STOP_C, STOP_H),
      testCase().points(STOP_C).expectTransfersFrom(STOP_C, STOP_H),
      testCase().points(STOP_D).expectTransfersFrom(STOP_E, STOP_J),
      testCase().points(STOP_E).expectTransfersFrom(STOP_E, STOP_J),
      testCase().points(STOP_I).expectTransfersFrom(STOP_C, STOP_J),
      testCase().points(STOP_J).expectTransfersFrom(STOP_C, STOP_J),
      testCase().points(STOP_L).expectTransfersFrom(STOP_C, STOP_H),
      testCase().points(STOP_K, STOP_D).expectTransfersFrom(STOP_E, STOP_J),
      testCase().points(STOP_K, STOP_E).expectTransfersFrom(STOP_E, STOP_J),
      testCase().points(STOP_K, STOP_I).expectTransfersFrom(STOP_C, STOP_J),
      testCase().points(STOP_K, STOP_J).expectTransfersFrom(STOP_C, STOP_J),
      testCase().points(STOP_K, STOP_L).expectTransfersFrom(STOP_C, STOP_J),
      testCase().points(STOP_K, STOP_M).expectTransfersFrom(STOP_C, STOP_J),
      testCase().points(STOP_G, STOP_B).expectTransfersFrom(STOP_E, STOP_J),
      testCase().points(STOP_G, STOP_C).expectTransfersFrom(STOP_E, STOP_J),
      testCase().points(STOP_G, STOP_D).expectTransfersFrom(STOP_E, STOP_J),
      testCase().points(STOP_G, STOP_E).expectTransfersFrom(STOP_E, STOP_J),
      testCase().points(STOP_G, STOP_I).expectTransfersFrom(STOP_E, STOP_J),
      testCase().points(STOP_G, STOP_J).expectTransfersFrom(STOP_E, STOP_J),
      testCase().points(STOP_G, STOP_L).expectTransfersFrom(STOP_E, STOP_J),
      testCase().points(STOP_G, STOP_M).expectTransfersFrom(STOP_E, STOP_J),
      testCase("at D w/unreachable A").points(STOP_D, STOP_A).expectTransfersFrom(STOP_E, STOP_J),
      testCase("at E w/unreachable A").points(STOP_E, STOP_A).expectTransfersFrom(STOP_E, STOP_J),
      testCase("at I w/unreachable A").points(STOP_I, STOP_A).expectTransfersFrom(STOP_C, STOP_J),
      testCase("at J w/unreachable A").points(STOP_J, STOP_A).expectTransfersFrom(STOP_C, STOP_J),
      testCase("at L w/unreachable A").points(STOP_L, STOP_A).expectTransfersFrom(STOP_C, STOP_H),
      testCase("at D w/unreachable F").points(STOP_D, STOP_F).expectTransfersFrom(STOP_E, STOP_J),
      testCase("at E w/unreachable F").points(STOP_E, STOP_F).expectTransfersFrom(STOP_E, STOP_J),
      testCase("at I w/unreachable F").points(STOP_I, STOP_F).expectTransfersFrom(STOP_C, STOP_J),
      testCase("at J w/unreachable F").points(STOP_J, STOP_F).expectTransfersFrom(STOP_C, STOP_J),
      testCase("at L w/unreachable F").points(STOP_L, STOP_F).expectTransfersFrom(STOP_C, STOP_H),
      // Two pass-through-points - a few samples
      testCase().points(STOP_B).points(STOP_C).expectTransfersFrom(STOP_C, STOP_H),
      testCase().points(STOP_B).points(STOP_D).expectTransfersFrom(STOP_E, STOP_J),
      testCase().points(STOP_B).points(STOP_E).expectTransfersFrom(STOP_E, STOP_J),
      testCase().points(STOP_B).points(STOP_G).expectTransfersFrom(STOP_C, STOP_H),
      testCase().points(STOP_B).points(STOP_H).expectTransfersFrom(STOP_C, STOP_H),
      testCase().points(STOP_B).points(STOP_I).expectTransfersFrom(STOP_C, STOP_J),
      testCase().points(STOP_B).points(STOP_J).expectTransfersFrom(STOP_C, STOP_J),
      testCase().points(STOP_B).points(STOP_K).expectTransfersFrom(STOP_C, STOP_H),
      testCase().points(STOP_B).points(STOP_L).expectTransfersFrom(STOP_C, STOP_H),
      testCase().points(STOP_C).points(STOP_D).expectTransfersFrom(STOP_E, STOP_J),
      testCase().points(STOP_C).points(STOP_E).expectTransfersFrom(STOP_E, STOP_J),
      testCase().points(STOP_C).points(STOP_G).expectTransfersFrom(STOP_C, STOP_H),
      testCase().points(STOP_C).points(STOP_H).expectTransfersFrom(STOP_C, STOP_H),
      testCase().points(STOP_C).points(STOP_I).expectTransfersFrom(STOP_C, STOP_J),
      testCase().points(STOP_C).points(STOP_J).expectTransfersFrom(STOP_C, STOP_J),
      testCase().points(STOP_C).points(STOP_K).expectTransfersFrom(STOP_C, STOP_H),
      testCase().points(STOP_C).points(STOP_L).expectTransfersFrom(STOP_C, STOP_H),
      testCase().points(STOP_D).points(STOP_E, STOP_K).expectTransfersFrom(STOP_E, STOP_J),
      testCase().points(STOP_D).points(STOP_I, STOP_H).expectTransfersFrom(STOP_E, STOP_J),
      testCase().points(STOP_D).points(STOP_J, STOP_K).expectTransfersFrom(STOP_E, STOP_J),
      testCase().points(STOP_D).points(STOP_L).expectTransfersFrom(STOP_E, STOP_J),
      testCase().points(STOP_D).points(STOP_M).expectTransfersFrom(STOP_E, STOP_J),
      testCase().points(STOP_E).points(STOP_I).expectTransfersFrom(STOP_E, STOP_J),
      testCase().points(STOP_E).points(STOP_J).expectTransfersFrom(STOP_E, STOP_J),
      testCase().points(STOP_E).points(STOP_L).expectTransfersFrom(STOP_E, STOP_J),
      testCase().points(STOP_E).points(STOP_M).expectTransfersFrom(STOP_E, STOP_J),
      testCase().points(STOP_G).points(STOP_H).expectTransfersFrom(STOP_C, STOP_H),
      testCase().points(STOP_G).points(STOP_I).expectTransfersFrom(STOP_C, STOP_J),
      testCase().points(STOP_G).points(STOP_J).expectTransfersFrom(STOP_C, STOP_J),
      testCase().points(STOP_G).points(STOP_K).expectTransfersFrom(STOP_C, STOP_H),
      testCase().points(STOP_G).points(STOP_L).expectTransfersFrom(STOP_C, STOP_H),
      testCase().points(STOP_G).points(STOP_M).expectTransfersFrom(STOP_C, STOP_H),
      testCase().points(STOP_H).points(STOP_I).expectTransfersFrom(STOP_C, STOP_J),
      testCase().points(STOP_H).points(STOP_J).expectTransfersFrom(STOP_C, STOP_J),
      testCase().points(STOP_H).points(STOP_K).expectTransfersFrom(STOP_C, STOP_H),
      testCase().points(STOP_H).points(STOP_L).expectTransfersFrom(STOP_C, STOP_H),
      testCase().points(STOP_H).points(STOP_M).expectTransfersFrom(STOP_C, STOP_H),
      testCase().points(STOP_I).points(STOP_J).expectTransfersFrom(STOP_C, STOP_J),
      testCase().points(STOP_I).points(STOP_L).expectTransfersFrom(STOP_C, STOP_J),
      testCase().points(STOP_I).points(STOP_M).expectTransfersFrom(STOP_C, STOP_J),
      testCase().points(STOP_J).points(STOP_L).expectTransfersFrom(STOP_C, STOP_J),
      testCase().points(STOP_J).points(STOP_M).expectTransfersFrom(STOP_C, STOP_J),
      testCase().points(STOP_K).points(STOP_L).expectTransfersFrom(STOP_C, STOP_H),
      testCase().points(STOP_K).points(STOP_M).expectTransfersFrom(STOP_C, STOP_H),
      testCase().points(STOP_L).points(STOP_M).expectTransfersFrom(STOP_C, STOP_H)
    );
  }

  /**
   * In this test we will test a path with *three* transit legs and 2 transfers. For each transfer
   * there is two options, and in total three possible paths:
   * <ol>
   *   <li>Origin ~ B ~ C ~ G ~ H ~ K ~ M ~ Destination</li>
   *   <li>Origin ~ B ~ C ~ G ~ J ~ L ~ M ~ Destination</li>
   *   <li>Origin ~ B ~ E ~ I ~ J ~ L ~ M ~ Destination</li>
   * </ol>
   * This is how the network look like:
   * <pre>
   *          Origin
   *                \
   *  Trip 1   A --- B --- C --- D --- E
   *                        \ 5s[20s]   \ 10s
   *  Trip 2           F --- G --- H --- I --- J
   *                               \ 5s[20s]    \ 10s
   *  Trip 3                        K ---------- L --- M
   *                                                    \
   *                                                     Destination
   * </pre>
   * The point in this test is to give the path with transfer C-G & H-K an advantage
   * (generalized-cost), but at the same time miss out on possible transfer-points (D,E,I,J).
   * <p>
   * If stop G or K is part of a pass-through-point, then we would like to make an exception to the
   * generalized-cost by increasing the cost for transfer K -> H-K and G -> C-G to 20s - making
   * these transfers less favorable on generalized-cost.
   * <p>
   * We will variate this test with zero, one and two pass-through point and by making unreachable
   * stops(A,F) part of the pass-through-points.
   */
  @ParameterizedTest
  @MethodSource("tripWithTwoTransferTestCases")
  public void tripWithTwoTransfer(TestCase tc) {
    // We set up the cost so that the maximum number of stops are skipped if we route on
    // generalized-cost only.
    final int costCG = tc.contains(STOP_G) ? 20 : 5;
    final int costEI = 10;
    final int costHK = tc.contains(STOP_K) ? 20 : 5;
    final int costJL = 10;

    // We need *a* path - the transfer her can be any
    var originalPath = pathBuilder()
      .access(ITERATION_START_TIME, STOP_B, D1s)
      .bus(trip1, STOP_C)
      .walk(costCG, STOP_G)
      .bus(trip2, STOP_H)
      .walk(costHK, STOP_K)
      .bus(trip3, STOP_M)
      .egress(D1s);

    RaptorPath<TestTripSchedule> expectedPath;
    {
      var b = pathBuilder().c2(tc.points().size()).access(ITERATION_START_TIME, STOP_B, D1s);

      if (tc.stopIndexA() == STOP_C) {
        b.bus(trip1, STOP_C).walk(costCG, STOP_G);
      } else {
        b.bus(trip1, STOP_E).walk(costEI, STOP_I);
      }
      if (tc.stopIndexB() == STOP_H) {
        b.bus(trip2, STOP_H).walk(costHK, STOP_K);
      } else {
        b.bus(trip2, STOP_J).walk(costJL, STOP_L);
      }
      expectedPath = b.bus(trip3, STOP_M).egress(D1s);
    }

    var firstTransfers = List.of(
      tx(trip1, STOP_C, trip2, STOP_G, costCG),
      tx(trip1, STOP_E, trip2, STOP_I, costEI)
    );
    var secondTransfers = List.of(
      tx(trip2, STOP_H, trip3, STOP_K, costHK),
      tx(trip2, STOP_J, trip3, STOP_L, costJL)
    );

    var subject = domainService(tc.points(), firstTransfers, secondTransfers);

    // When

    var result = subject.findBestTransitPath(originalPath);

    // Then expect a set containing the expected path only
    assertEquals(
      expectedPath.toString(this::stopIndexToName),
      // Remove transferPriority cost
      PathUtils.pathsToString(result).replace(" Tₚ6_600", "")
    );
  }
}
