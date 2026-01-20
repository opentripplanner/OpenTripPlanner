package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_A;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_B;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_C;
import static org.opentripplanner.raptor._data.RaptorTestConstants.T00_00;
import static org.opentripplanner.raptor._data.RaptorTestConstants.T01_00;
import static org.opentripplanner.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.raptor.api.request.RaptorViaLocation.via;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorProfile;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.api.request.RaptorViaLocation;
import org.opentripplanner.raptor.configure.RaptorTestFactory;
import org.opentripplanner.raptor.moduletests.support.ModuleTestDebugLogging;

/**
 * FEATURE UNDER TEST
 * <p>
 * Raptor should support via-visit searches where access paths visit via locations.
 * <p>
 * This allows for scenarios like:
 * <ul>
 *   <li>Walking through a via location before boarding transit</li>
 *   <li>Using flex to visit via locations</li>
 * </ul>
 */
class J04_ViaVisitWithAccessTest {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = data.requestBuilder();
  private final RaptorService<TestTripSchedule> raptorService = RaptorTestFactory.raptorService();

  @BeforeEach
  void setup() {
    var builder = data.requestBuilder();

    builder
      .profile(RaptorProfile.MULTI_CRITERIA)
      // TODO: Currently heuristics does not work with via-visit so we turn them off
      .clearOptimizations();

    builder
      .searchParams()
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T01_00)
      .searchWindow(Duration.ofMinutes(10))
      .timetable(true);

    ModuleTestDebugLogging.setupDebugLogging(data);
  }

  @Test
  @DisplayName("Access VIA stop A, no other options. Raptor should skip the first via point.")
  void accessViaVisit() {
    data
      .access("Walk 4m Vₙ1 ~ B")
      .withTimetables(
        """
        B     C
        0:10  0:20
        """
      )
      .egress("C ~ Walk 30s");

    requestBuilder.searchParams().addViaLocation(viaStop("Via-A", STOP_A));

    // The via point is visited in the access-path.
    // There is no check inside Raptor that the specific location A is visited.
    var result = raptorService.route(requestBuilder.build(), data);

    assertEquals(
      "Walk 4m Vₙ1 ~ B ~ BUS R1 0:10 0:20 ~ C ~ Walk 30s [0:06 0:20:30 14m30s Tₙ0 C₁1_740]",
      pathsToString(result)
    );
  }

  @Test
  @DisplayName("Access VIA stop A, with other less optimal options.")
  void accessViaVisitWithFasterDirectAccess() {
    data
      .access(
        // Higher cost
        "Walk 2m ~ A",
        // Does not visit B
        "Walk 2m ~ C",
        // Optimal
        "Walk 4m Vₙ1 ~ C"
      )
      .withTimetables(
        """
        A     B     C     D
        0:05  0:10  0:20  0:30
        """
      )
      .egress("D ~ Walk 30s");

    requestBuilder.searchParams().addViaLocation(viaStop("Via-B", STOP_B));

    var result = raptorService.route(requestBuilder.build(), data);

    assertEquals(
      "Walk 4m Vₙ1 ~ C ~ BUS R1 0:20 0:30 ~ D ~ Walk 30s [0:16 0:30:30 14m30s Tₙ0 C₁1_740]",
      pathsToString(result)
    );
  }

  @Test
  @DisplayName(
    "Visit via is in access. This tests the corner case where two access paths both arrive at via " +
    "stop. One has the numberOfViaLocationsVisited() set, the other not. It is NICE TO HAVE to " +
    "handle this gracefully."
  )
  void accessWithViaVisit() {
    data
      .access("Walk 4m Vₙ1 ~ B", "Walk 1m ~ B")
      .withTimetables(
        """
        B     C
        0:10  0:20
        """
      )
      .egress("C ~ Walk 30s");

    requestBuilder.searchParams().addViaLocation(viaStop("Via-B", STOP_B));

    var result = raptorService.route(requestBuilder.build(), data);

    assertEquals(
      "Walk 1m ~ B ~ BUS R1 0:10 0:20 ~ C ~ Walk 30s [0:09 0:20:30 11m30s Tₙ0 C₁1_380]",
      pathsToString(result)
    );
  }

  @Test
  @DisplayName(
    "Access visits first via location (A), then second via location C is visited using transit"
  )
  void accessVisitsFirstViaLocation() {
    data
      .access("Walk 2m Vₙ1 ~ B", "Walk 1m ~ B")
      .withTimetables(
        """
         B    C
        0:05  0:15
        --
              C     D
              0:25  0:35
        """
      )
      .egress("D ~ Walk 1m");

    requestBuilder
      .searchParams()
      // Define two via locations - A is visited by the access and the other is visited with Raptor
      .addViaLocation(viaStop("Via-A", STOP_A))
      .addViaLocation(viaStop("Via-C", STOP_C));

    var result = raptorService.route(requestBuilder.build(), data);

    assertEquals(
      "Walk 2m Vₙ1 ~ B ~ BUS R1 0:05 0:15 ~ C ~ BUS R2 0:25 0:35 ~ D ~ Walk 1m [0:03 0:36 33m Tₙ1 C₁3_360]",
      pathsToString(result)
    );
  }

  private static RaptorViaLocation viaStop(String label, int... stops) {
    var builder = via(label);
    for (int stop : stops) {
      builder.addViaStop(stop);
    }
    return builder.build();
  }
}
