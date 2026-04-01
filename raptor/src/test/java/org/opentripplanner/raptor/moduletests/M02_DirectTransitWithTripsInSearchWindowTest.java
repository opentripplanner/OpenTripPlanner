package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.api.PathUtils.pathsToString;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.configure.RaptorTestFactory;
import org.opentripplanner.raptor.direct.api.RaptorDirectTransitRequest;

/**
 * FEATURE UNDER TEST
 * <p>
 * The direct transit search should return all trips on the same route within the search window.
 */
public class M02_DirectTransitWithTripsInSearchWindowTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorService<TestTripSchedule> raptorService = RaptorTestFactory.raptorService();

  @BeforeEach
  void setup() {
    data.withTimetable(
      "R1",
      // The searchWindow is set to 00:00 to 00:03, so with 30s access the last trip starts after
      // the latest-depature-time.
      """
      A      B
      00:02  00:04
      00:03  00:05
      00:04  00:06
      """
    );
  }

  @Test
  void testDirectTransitSearchWindow() {
    var request = RaptorDirectTransitRequest.of()
      .earliestDepartureTime(T00_00)
      .searchWindowInSeconds(D3_m)
      .addAccessPaths(TestAccessEgress.walk(STOP_A, D30_s))
      .addEgressPaths(TestAccessEgress.walk(STOP_B, D20_s))
      .build();

    var result = raptorService.findAllDirectTransit(request, data);
    assertEquals(
      """
      Walk 30s ~ A ~ BUS R1 0:02 0:04 ~ B ~ Walk 20s [0:01:30 0:04:20 2m50s Tₙ0 C₁820]
      Walk 30s ~ A ~ BUS R1 0:03 0:05 ~ B ~ Walk 20s [0:02:30 0:05:20 2m50s Tₙ0 C₁820]""",
      pathsToString(result)
    );
  }
}
