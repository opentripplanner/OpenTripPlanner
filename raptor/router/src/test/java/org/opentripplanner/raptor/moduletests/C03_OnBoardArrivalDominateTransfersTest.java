package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.transit.TestTransfer.transfer;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_MIN_DURATION;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_MIN_DURATION_REV;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.multiCriteria;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.standard;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.api.PathUtils;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorTestFactory;
import org.opentripplanner.raptor.moduletests.support.RaptorModuleTestCase;

/**
 * FEATURE UNDER TEST
 * <p>
 * Egress stops must use the 4D Pareto comparator (arrivalTime, paretoRound, cost,
 * arrivedOnBoard). Regular transit intermediary stops use the 3D comparator (no arrivedOnBoard
 * dimension).
 * <p>
 * At stop C, two arrivals compete:
 * <ol>
 *   <li>Walk-transfer via R1(A→B) + walk B→C: round=1, arrives ~0:09, cost low,
 *       arrivedOnBoard=false — does NOT fire walk egress</li>
 *   <li>Transit via R1(A→B) + R2(B→C): round=2, arrives 0:15, cost higher,
 *       arrivedOnBoard=true — fires walk egress</li>
 * </ol>
 * Arrival 1 strictly dominates arrival 2 in all three base dimensions (3D), so under the 3D
 * comparator the transit is rejected and the walk egress never fires. The 4D comparator at C
 * preserves both arrivals and finds the path.
 */
public class C03_OnBoardArrivalDominateTransfersTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = data.requestBuilder();
  private final RaptorService<TestTripSchedule> raptorService = RaptorTestFactory.raptorService();

  @BeforeEach
  public void setup() {
    data
      .access("Walk 1m ~ A")
      .withTimetables(
        """
        -- R1
        A     B
        0:05  0:08
        -- R2
              B     C
              0:12  0:15
        """
      )
      .egress("C ~ Walk 1m")
      // We add a transfer here which arrive at C before R2, but it should not be used.
      .withTransfer(STOP_B, transfer(STOP_C, D1_m));

    requestBuilder
      .searchParams()
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T00_30)
      .searchWindowInSeconds(D10_m);
  }

  static List<RaptorModuleTestCase> testCases() {
    var path =
      "Walk 1m ~ A ~ " +
      "BUS R1 0:05 0:08 ~ B ~ " +
      "BUS R2 0:12 0:15 ~ C ~ " +
      "Walk 1m " +
      "[0:04 0:16 12m Tₙ1 C₁2_040]";

    return RaptorModuleTestCase.of()
      // Zero transfers is wrong, it is caused by the egress stop(C) being reached by transfer
      // with 0tx(before boarding), so 0 transfers is stored in BestNumberOfTransfers for stop C.
      // Then since the path is computed after, not during, the Raptor search this fails combine
      // the egress with the correct path. The result is still valid to use as heuristics, so it
      // does not cause the mc-raptor w/destination pruning to miss results. There are two ways to
      // fix this. We can keep the best-number-of-transfers for both transit and over-all for all
      // stops (at least egress stops) or we can compute paths during the search. The last
      // solution is probably the one which give the best performance, but the first will make
      // mc-raptor perform better (tighter heuristics).
      .add(TC_MIN_DURATION, "[0:00 0:09 9m Tₙ0]")
      .add(TC_MIN_DURATION_REV, "[0:21 0:30 9m Tₙ1]")
      .add(standard(), PathUtils.withoutCost(path))
      .add(multiCriteria(), path)
      .build();
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void test(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }
}
