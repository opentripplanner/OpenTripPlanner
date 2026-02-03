package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_B;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_C;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_D;
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
import org.opentripplanner.raptor.configure.RaptorTestFactory;
import org.opentripplanner.raptor.moduletests.support.ModuleTestDebugLogging;

/**
 * FEATURE UNDER TEST
 * <p>
 * Raptor should support via-visit searches where egress paths visit via locations.
 * <p>
 * This allows for scenarios like:
 * <ul>
 *   <li>Walking through a via location after alighting last transit</li>
 *   <li>Support for using via in flex</li>
 * </ul>
 */
class J05_ViaVisitWithEgressTest {

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
  @DisplayName("Egress VIA stop C, no other options.")
  void viaVisitSimpleCase() {
    data
      .access("Walk 1m ~ A")
      .withTimetables(
        """
        A     B     C
        0:10  0:20  0:30
        """
      )
      .egress("B ~ Walk 30s Vₙ1");

    viaStop(STOP_C);

    // The via point is visited in the egress-path.
    // There is no check inside Raptor that the specific location C is visited.
    var result = raptorService.route(requestBuilder.build(), data);

    assertEquals(
      "Walk 1m ~ A ~ BUS R1 0:10 0:20 ~ B ~ Walk 30s Vₙ1 [0:09 0:20:30 11m30s Tₙ0 C₁1_380]",
      pathsToString(result)
    );
  }

  @Test
  @DisplayName("Optimal egress including VIA, dominating other less favourable egress options.")
  void viaVisitWithEgressViaAsTheOptimalOption() {
    data
      .access("Walk 30s ~ A")
      .withTimetables(
        """
        A     B     C     D
        0:05  0:10  0:20  0:30
        """
      )
      .egress(
        // Higher cost
        "D ~ Walk 2m",
        // Does not visit C
        "B ~ Walk 2m",
        // Optimal - Via point in egress
        "B ~ Walk 4m Vₙ1"
      );

    viaStop(STOP_C);

    var result = raptorService.route(requestBuilder.build(), data);

    assertEquals(
      "Walk 30s ~ A ~ BUS R1 0:05 0:10 ~ B ~ Walk 4m Vₙ1 [0:04:30 0:14 9m30s Tₙ0 C₁1_440]",
      pathsToString(result)
    );
  }

  @Test
  @DisplayName(
    "Find two paths using optimal egresses, one egress including and one without the via-location"
  )
  void visitViaUsingTransitNotEgress() {
    data
      .access("Walk 1m ~ A")
      .withTimetables(
        """
        A     B     C
        0:05  0:10  0:15
        --
                    C      D
                    0:18  0:20
        """
      )
      .egress(
        // Optimal,
        "D ~ Walk 1m",
        // Optimal - Visit via in egress. Lowest number of transfers.
        "B ~ Walk 15m Vₙ1",
        // Does not visit C
        "B ~ Walk 1m"
      );

    viaStop(STOP_C);

    var result = raptorService.route(requestBuilder.build(), data);

    assertEquals(
      """
      Walk 1m ~ A ~ BUS R1 0:05 0:15 ~ C ~ BUS R2 0:18 0:20 ~ D ~ Walk 1m [0:04 0:21 17m Tₙ1 C₁2_340]
      Walk 1m ~ A ~ BUS R1 0:05 0:10 ~ B ~ Walk 15m Vₙ1 [0:04 0:25 21m Tₙ0 C₁2_820]""",
      pathsToString(result)
    );
  }

  @Test
  @DisplayName(
    """
    This tests the corner case where two egress paths both depart from the via stop location, one
    has the numberOfViaLocationsVisited() set, the other not. Officially Raptor only supports
    access/egress where the stop is NOT part of the count. But, the implementation supports both
    cases and there is no reason to restrict it. To handle this gracefully, is NICE TO HAVE."""
  )
  void accessWithViaVisit() {
    data
      .access("Walk 1m ~ A")
      .withTimetables(
        """
        A     B
        0:10  0:20
        """
      )
      .egress(
        // Optimal on cost
        "B ~ Walk 2m C₁100 Vₙ1",
        // Optimal on time
        "B ~ Walk 1m C₁200"
      );

    viaStop(STOP_B);

    var result = raptorService.route(requestBuilder.build(), data);

    assertEquals(
      """
      Walk 1m ~ A ~ BUS R1 0:10 0:20 ~ B ~ Walk 1m [0:09 0:21 12m Tₙ0 C₁1_520]
      Walk 1m ~ A ~ BUS R1 0:10 0:20 ~ B ~ Walk 2m Vₙ1 [0:09 0:22 13m Tₙ0 C₁1_420]""",
      pathsToString(result)
    );
  }

  @Test
  @DisplayName("Combine via locations in transit and egress")
  void combineViaLocationsInTransitAndEgress() {
    data
      .access("Walk 1m ~ A")
      .withTimetables(
        """
        A     B     C     D
        0:05  0:15  0:25  10
        0:15  0:25  0:35  11
        """
      )
      .egress("C ~ Walk 2m Vₙ1 C₁100", "C ~ Walk 1m C₁200");

    // Define two via locations - B is visited by transit and D is visited in the egress
    viaStop(STOP_B);
    viaStop(STOP_D);

    var result = raptorService.route(requestBuilder.build(), data);

    assertEquals(
      """
      Walk 1m ~ A ~ BUS R1 0:05 0:15 ~ B ~ BUS R1 0:25 0:35 ~ C ~ Walk 2m Vₙ1 [0:04 0:37 33m Tₙ1 C₁3_220]""",
      pathsToString(result)
    );
  }

  private void viaStop(int stop) {
    var via = via(data.stopNameResolver().apply(stop)).addViaStop(stop).build();
    requestBuilder.searchParams().addViaLocation(via);
  }

  private void viaStops(String label, int... stops) {
    var builder = via(label);
    for (int stop : stops) {
      builder.addViaStop(stop);
    }
    requestBuilder.searchParams().addViaLocation(builder.build());
  }
}
