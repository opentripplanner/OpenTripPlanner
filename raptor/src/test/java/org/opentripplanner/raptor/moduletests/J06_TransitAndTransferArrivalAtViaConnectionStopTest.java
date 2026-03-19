package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.raptor._data.transit.TestTransfer.transfer;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorProfile;
import org.opentripplanner.raptor.api.request.RaptorViaLocation;
import org.opentripplanner.raptor.configure.RaptorTestFactory;

/**
 * FEATURE UNDER TEST
 * <p>
 * Via-connection stops (the FROM-stop of a cross-stop via transfer) must use the 4D Pareto
 * comparator (arrivalTime, paretoRound, cost, arrivedOnBoard), for the same reason as egress
 * stops: the via-transfer-connection is only triggered by a TRANSIT arrival (arrivedOnBoard=true),
 * not by a walk-transfer arrival (arrivedOnBoard=false).
 * <p>
 * <b>The bug scenario (3D at via-connection stop D):</b> A walk-transfer arrival at D is
 * established in round 1, arriving earlier and cheaper than the round-2 transit arrival via R2.
 * Under the 3D comparator the transfer (round=1) strictly dominates the transit (round=2). The
 * transit arrival is rejected — the via connection D→F never fires — and MC routing returns an
 * empty result.
 * <p>
 * <b>The fix (4D at via-connection stop D):</b> The transit arrival wins on
 * {@code arrivedOnBoard=true}, creating mutual dominance. Both arrivals coexist. The transit
 * fires the via connection and the path is found.
 * <p>
 * At stop D (the via FROM-stop), two arrivals compete:
 * <ol>
 *   <li>Walk-transfer via R1(A→B) + walk B→D: round=1, arrives 0:09, cost low,
 *       arrivedOnBoard=false — does NOT fire the via connection to F</li>
 *   <li>Transit via R1(A→C) + R2(C→D): round=2, arrives 0:15, cost higher,
 *       arrivedOnBoard=true — fires the via connection to F</li>
 * </ol>
 * Arrival 1 strictly dominates arrival 2 in all three base dimensions (3D), so under the 3D
 * comparator the transit is rejected and the via connection never fires. The 4D comparator at D
 * preserves both arrivals and finds the path.
 */
class J06_TransitAndTransferArrivalAtViaConnectionStopTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorService<TestTripSchedule> raptorService = RaptorTestFactory.raptorService();

  @Test
  void transitArrivalAtViaConnectionStopNotBlockedByEarlierWalkTransfer() {
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
        -- R3
                                F     G
                                0:17  0:22
        """
      )
      .egress("G ~ Walk 1m")
      // Regular walk transfer B→D — creates a round-1 walk arrival at D (the via FROM-stop)
      // that, without the 4D fix, would block the round-2 transit arrival and prevent the via
      // connection from firing.
      .withTransfer(STOP_B, transfer(STOP_D, D2_m));

    var requestBuilder = data.requestBuilder();
    requestBuilder
      .profile(RaptorProfile.MULTI_CRITERIA)
      // TODO: 2023-07-24 Currently heuristics does not work with via search so we
      //  have to turn them off. Make sure to re-enable optimization later when it's fixed.
      .clearOptimizations();

    requestBuilder
      .searchParams()
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T00_30)
      .searchWindow(Duration.ofMinutes(10))
      .timetable(true)
      .addViaLocation(
        RaptorViaLocation.via("D→F").addViaTransfer(STOP_D, transfer(STOP_F, D1_m)).build()
      );

    assertEquals(
      "Walk 1m ~ A ~ BUS R1 0:05 0:08 ~ C ~ BUS R2 0:10 0:15 ~ D ~ Walk 1m ~ F " +
        "~ BUS R3 0:17 0:22 ~ G ~ Walk 1m [0:04 0:23 19m Tₙ2 C₁3_120]",
      pathsToString(raptorService.route(requestBuilder.build(), data))
    );
  }
}
