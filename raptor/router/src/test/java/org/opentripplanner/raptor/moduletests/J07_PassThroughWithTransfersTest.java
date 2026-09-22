package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.raptor._data.transit.TestTransfer.transfer;
import static org.opentripplanner.raptor.api.request.via.RaptorViaLocation.passThrough;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorProfile;
import org.opentripplanner.raptor.configure.RaptorTestFactory;

/**
 * FEATURE UNDER TEST
 * <p>
 * A pass-through via stop is satisfied only by a transit arrival (boarding or alighting a
 * vehicle). A walk-transfer arrival at the via stop does not satisfy the pass-through constraint,
 * even if it arrives earlier and at lower cost than the transit arrival.
 */
class J07_PassThroughWithTransfersTest implements RaptorTestConstants {

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
              D     E
              0:12  0:18
        -- R3
                    F     G
                    0:22  0:25
        """
      )
      .egress("G ~ Walk 1m")
      // Walk B→D — allows boarding R2 at D toward the pass-through stop E
      .withTransfer(STOP_B, transfer(STOP_D, D2_m))
      // Walk C→E — creates an earlier walk arrival at E that could block R2's transit arrival
      .withTransfer(STOP_C, transfer(STOP_E, D2_m))
      // Walk E→F — continuation from E after the pass-through, enabling boarding R3
      .withTransfer(STOP_E, transfer(STOP_F, D2_m));

    var requestBuilder = data.requestBuilder();
    requestBuilder
      .profile(RaptorProfile.MULTI_CRITERIA)
      // TODO: 2023-07-24 Currently heuristics does not work with via search so we
      //  have to turn them off. Make sure to re-enable optimization later when it's fixed.
      .clearOptimizations();

    requestBuilder
      .searchParams()
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T01_00)
      .searchWindow(Duration.ofMinutes(10))
      .timetable(true)
      .addViaLocation(passThrough("E").addStop(STOP_E).build());

    assertEquals(
      "Walk 1m ~ A ~ BUS R1 0:05 0:06 ~ B ~ Walk 2m ~ D ~ BUS R2 0:12 0:18 ~ E" +
        " ~ Walk 2m ~ F ~ BUS R3 0:22 0:25 ~ G ~ Walk 1m [0:04 0:26 22m Tₙ2 C₁3_480]",
      pathsToString(raptorService.route(requestBuilder.build(), data))
    );
  }
}
