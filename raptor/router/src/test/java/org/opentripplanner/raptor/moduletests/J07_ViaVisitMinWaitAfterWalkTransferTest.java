package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_B;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_C;
import static org.opentripplanner.raptor._data.RaptorTestConstants.T00_00;
import static org.opentripplanner.raptor._data.RaptorTestConstants.T01_00;
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
import org.opentripplanner.raptor.api.request.RaptorRequest;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorTestFactory;
import org.opentripplanner.raptor.spi.SearchDirection;

/**
 * FEATURE UNDER TEST
 * <p>
 * The stop of a via-visit location is reached by a regular walk transfer, and the location has a
 * minimum wait time. The transfer leg in the path must have the duration of the transfer. The
 * minimum wait time is added to the arrival time of the walk arrival in the search state, but it
 * is not a part of the walk.
 */
class J07_ViaVisitMinWaitAfterWalkTransferTest implements RaptorTestConstants {

  private static final Duration MIN_WAIT_TIME = Duration.ofMinutes(2);

  private final TestTransitData data = new TestTransitData();
  private final RaptorService<TestTripSchedule> raptorService = RaptorTestFactory.raptorService();

  @Test
  void forwardSearch() {
    var transitData = data();
    assertEquals(
      "Walk 1m ~ A ~ BUS R1 0:05 0:08 ~ B ~ Walk 1m ~ C ~ BUS R2 0:15 0:20 ~ D ~ Walk 1m " +
        "[0:04 0:21 17m Tₙ1 C₁2_400]",
      pathsToString(raptorService.route(request(SearchDirection.FORWARD), transitData))
    );
  }

  @Test
  void reverseSearch() {
    var transitData = data();
    assertEquals(
      "Walk 1m ~ A ~ BUS R1 0:05 0:08 ~ B ~ Walk 1m ~ C ~ BUS R2 0:15 0:20 ~ D ~ Walk 1m " +
        "[0:04 0:21 17m Tₙ1 C₁2_400]",
      pathsToString(raptorService.route(request(SearchDirection.REVERSE), transitData))
    );
  }

  private TestTransitData data() {
    return data
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
  }

  private RaptorRequest<TestTripSchedule> request(SearchDirection direction) {
    RaptorRequestBuilder<TestTripSchedule> builder = data.requestBuilder();
    builder
      .profile(RaptorProfile.MULTI_CRITERIA)
      .searchDirection(direction)
      // Heuristics does not work with via search
      .clearOptimizations();
    builder
      .searchParams()
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T01_00)
      .searchWindow(Duration.ofMinutes(10))
      .timetable(true)
      .addViaLocation(viaVisit("C", MIN_WAIT_TIME).addStop(STOP_C).build());
    return builder.build();
  }
}
