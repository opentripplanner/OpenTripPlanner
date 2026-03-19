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
 * {@code CalculateTransferToDestination} fires egresses based on how you arrived:
 * <ul>
 *   <li>TRANSIT arrival (arrivedOnBoard=true): fires ALL egresses (walk and flex)</li>
 *   <li>TRANSFER arrival (arrivedOnBoard=false): fires only stopReachedOnBoard=true egresses</li>
 * </ul>
 * A walk egress (stopReachedOnBoard=false) is therefore ONLY triggered by transit arrivals.
 * <p>
 * <b>The bug scenario (3D at egress stop D):</b> A walk-transfer arrival at D is established in
 * round 1, arriving earlier and cheaper than the round-2 transit arrival via a second route. Under
 * the 3D comparator the transfer (round=1) strictly dominates the transit (round=2) in all three
 * dimensions: earlier time, fewer rounds, lower cost. The transit arrival is rejected — the walk
 * egress never fires — and MC routing returns an empty result.
 * <p>
 * <b>The fix (4D at egress stop D):</b> The transit arrival wins on {@code arrivedOnBoard=true},
 * creating mutual dominance. Both arrivals coexist. The transit fires the walk egress and the path
 * is found.
 * <p>
 * Network:
 * <pre>
 *   Walk 1m
 * O -------> A -[R1]-> B -[R1]-> C
 *                      |         |
 *               Walk 2m|         |-[R2]-> D ~ Walk 1m -> destination
 *                      |                  ^
 *                      +------ Walk ------+
 * </pre>
 * At stop D, two arrivals compete:
 * <ol>
 *   <li>Walk-transfer via R1(A→B) + walk B→D: round=1, arrives 0:08, cost low,
 *       arrivedOnBoard=false — does NOT fire walk egress</li>
 *   <li>Transit via R1(A→C) + R2(C→D): round=2, arrives 0:15, cost higher,
 *       arrivedOnBoard=true — fires walk egress</li>
 * </ol>
 * Arrival 1 strictly dominates arrival 2 in all three base dimensions (3D), so under the 3D
 * comparator the transit is rejected and the walk egress never fires. The 4D comparator at D
 * preserves both arrivals and finds the path.
 * <p>
 * Note: both standard and MC routing reach the destination only via R1→R2, since the walk egress
 * from D is only triggered by a transit (on-board) arrival — not by the walk-transfer from B.
 */
public class C04_TransitAndTransferArrivalAtEgressStopTest implements RaptorTestConstants {

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
        A     B     C
        0:05  0:06  0:08
        -- R2
        C     D
        0:10  0:15
        """
      )
      .egress("D ~ Walk 1m")
      .withTransfer(STOP_B, transfer(STOP_D, D2_m));

    requestBuilder
      .searchParams()
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T00_30)
      .searchWindowInSeconds(D10_m);
  }

  static List<RaptorModuleTestCase> testCases() {
    // Both standard and MC routing reach D only via transit (R1→R2): the walk egress from D is
    // only triggered by a transit (on-board) arrival, not by a walk-transfer arrival.
    var path =
      "Walk 1m ~ A ~ BUS R1 0:05 0:08 ~ C ~ BUS R2 0:10 0:15 ~ D ~ Walk 1m [0:04 0:16 12m Tₙ1]";

    // MC routing (with 4D fix) finds the path via R1+R2 where the transit arrival at D fires
    // the walk egress. Without the fix (3D), the round-1 walk-transfer arrival at D strictly
    // dominates the round-2 transit arrival, rejecting it — walk egress never fires — empty result.
    var mcPath =
      "Walk 1m ~ A ~ BUS R1 0:05 0:08 ~ C ~ BUS R2 0:10 0:15 ~ D ~ Walk 1m [0:04 0:16 12m Tₙ1 C₁2_040]";

    return RaptorModuleTestCase.of()
      .add(TC_MIN_DURATION, "[0:00 0:11 11m Tₙ0]")
      .add(TC_MIN_DURATION_REV, "[0:19 0:30 11m Tₙ1]")
      .add(standard(), PathUtils.withoutCost(path))
      .add(multiCriteria(), mcPath)
      .build();
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void test(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }
}
