package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.transit.TestTransfer.transfer;
import static org.opentripplanner.raptor.api.request.via.RaptorViaLocation.viaVisit;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.path.PathLeg;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.api.request.RaptorProfile;
import org.opentripplanner.raptor.configure.RaptorTestFactory;

/**
 * FEATURE UNDER TEST
 * <p>
 * A via location can have a visit connection and a transfer connection starting at the same stop.
 * The two paths below both satisfy the location, and the first leg after the via stop is a transfer
 * to D in both. The text of the paths is the same, but one is the regular transfer and the other
 * is the via transfer:
 * <ul>
 *   <li>Visit B, then the regular walk B~D. It takes 4m, so the first trip from D is missed. The
 *       cost is low.</li>
 *   <li>The via transfer B~D. It takes 30s, so the first trip from D is caught. The cost is high.
 *       </li>
 * </ul>
 * Neither path dominates the other, so both are returned. The path mapper must create a leg for the
 * via transfer which is different from the leg for the regular transfer, even if the state has
 * the same stops and the same type of arrival for both.
 */
class J09_ViaVisitAndViaTransferInSameLocationTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorService<TestTripSchedule> raptorService = RaptorTestFactory.raptorService();

  @Test
  void viaVisitAndViaTransferPathsAreBothReturned() {
    var viaTransfer = transfer(STOP_D, 30, 200_000);
    data
      .access("Walk 1m ~ A")
      .withTimetables(
        """
        -- R1
        A     B
        0:05  0:08
        -- R2
                    D     E
                    0:12  0:15
                    0:20  0:23
        """
      )
      .withTransfer(STOP_B, transfer(STOP_D, D4_m))
      .egress("E ~ Walk 1m");

    var requestBuilder = data.requestBuilder();
    requestBuilder.profile(RaptorProfile.MULTI_CRITERIA).clearOptimizations();
    requestBuilder
      .searchParams()
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T01_00)
      .searchWindow(Duration.ofMinutes(10))
      .timetable(true)
      .addViaLocation(
        viaVisit("B", Duration.ofMinutes(1))
          .addStop(STOP_B)
          .addTransfer(STOP_B, viaTransfer)
          .build()
      );

    var paths = raptorService.route(requestBuilder.build(), data).paths();

    // The transfer is a via transfer in the first path and a regular transfer in the second
    assertEquals(
      List.of(
        "Walk 1m ~ A ~ BUS R1 0:05 0:08 ~ B ~ Walk 30s ~ D ~ BUS R2 0:12 0:15 ~ E ~ Walk 1m " +
          "[0:04 0:16 12m Tₙ1 C₁4_010] via-transfer=true",
        "Walk 1m ~ A ~ BUS R1 0:05 0:08 ~ B ~ Walk 4m ~ D ~ BUS R2 0:20 0:23 ~ E ~ Walk 1m " +
          "[0:04 0:24 20m Tₙ1 C₁2_760] via-transfer=false"
      ),
      paths.stream().map(J09_ViaVisitAndViaTransferInSameLocationTest::describe).sorted().toList()
    );
  }

  private static String describe(RaptorPath<TestTripSchedule> path) {
    var viaTransfer = path
      .legStream()
      .filter(PathLeg::isTransferLeg)
      .map(PathLeg::asTransferLeg)
      .anyMatch(it -> it.viaTransfer() != null);
    return path.toString(RaptorTestConstants::stopIndexToName) + " via-transfer=" + viaTransfer;
  }
}
