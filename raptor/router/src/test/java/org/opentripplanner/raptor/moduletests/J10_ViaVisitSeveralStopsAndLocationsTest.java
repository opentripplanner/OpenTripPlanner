package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.raptor._data.transit.TestTransfer.transfer;
import static org.opentripplanner.raptor.api.request.via.RaptorViaLocation.viaVisit;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorProfile;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorTestFactory;

/**
 * FEATURE UNDER TEST
 * <p>
 * The minimum wait time of a via visit is added to the arrival time, but it is not part of the
 * transfer. The transfer leg must have the duration of the transfer in these cases:
 * <ul>
 *   <li>The via location has several stops, and the path passes one of them on foot without
 *       visiting it. The wait time is added at the stop where the via location is visited.</li>
 *   <li>Two via locations are visited at the same stop, so the wait times are added together.</li>
 * </ul>
 */
class J10_ViaVisitSeveralStopsAndLocationsTest implements RaptorTestConstants {

  private static final Duration MIN_WAIT_TIME = Duration.ofMinutes(2);

  private final TestTransitData data = new TestTransitData();
  private final RaptorService<TestTripSchedule> raptorService = RaptorTestFactory.raptorService();

  @Test
  void aWalkToAStopOfTheViaLocationWhichIsNotTheVisit() {
    // The walk to C is too late to wait 2m at C before R2 leaves, so the via location is visited
    // at D. The walk to C is a regular transfer without any wait time.
    data
      .access("Walk 1m ~ A")
      .withTimetables(
        """
        -- R1
        A     B
        0:05  0:08
        -- R2
                    C     D
                    0:10  0:14
        -- R3
                          D     E
                          0:20  0:25
        """
      )
      .withTransfer(STOP_B, transfer(STOP_C, D1_m))
      .egress("E ~ Walk 1m");

    var builder = requestBuilder();
    builder
      .searchParams()
      .addViaLocation(viaVisit("C or D", MIN_WAIT_TIME).addStop(STOP_C).addStop(STOP_D).build());

    assertEquals(
      "Walk 1m ~ A ~ BUS R1 0:05 0:08 ~ B ~ Walk 1m ~ C ~ BUS R2 0:10 0:14 ~ D ~ " +
        "BUS R3 0:20 0:25 ~ E ~ Walk 1m [0:04 0:26 22m Tₙ2 C₁3_300]",
      pathsToString(raptorService.route(builder.build(), data))
    );
  }

  @Test
  void twoViaLocationsVisitedAtTheSameStop() {
    data
      .access("Walk 1m ~ A")
      .withTimetables(
        """
        -- R1
        A     B
        0:05  0:08
        -- R2
                    C     D
                    0:15  0:20
        """
      )
      .withTransfer(STOP_B, transfer(STOP_C, D1_m))
      .egress("D ~ Walk 1m");

    var builder = requestBuilder();
    builder
      .searchParams()
      .addViaLocation(viaVisit("C", MIN_WAIT_TIME).addStop(STOP_C).build())
      .addViaLocation(viaVisit("C again", MIN_WAIT_TIME).addStop(STOP_C).build());

    assertEquals(
      "Walk 1m ~ A ~ BUS R1 0:05 0:08 ~ B ~ Walk 1m ~ C ~ BUS R2 0:15 0:20 ~ D ~ Walk 1m " +
        "[0:04 0:21 17m Tₙ1 C₁2_400]",
      pathsToString(raptorService.route(builder.build(), data))
    );
  }

  private RaptorRequestBuilder<TestTripSchedule> requestBuilder() {
    var builder = data.requestBuilder();
    // Heuristics does not work with via search
    builder.profile(RaptorProfile.MULTI_CRITERIA).clearOptimizations();
    builder
      .searchParams()
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T01_00)
      .searchWindow(Duration.ofMinutes(10))
      .timetable(true);
    return builder;
  }
}
