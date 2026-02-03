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
 * The direct transit search should only return trips in the search window
 */
public class M03_DirectTransitSearchWindow implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorService<TestTripSchedule> raptorService = RaptorTestFactory.raptorService();

  @BeforeEach
  void setup() {
    data.withTimetable(
      "R1",
      """
      A     B
      00:02 00:03
      00:03 00:04
      00:04 00:05
      00:05 00:06
      """
    );
  }

  @Test
  void testRelaxedSearchWindow() {
    var request = RaptorDirectTransitRequest.of()
      .earliestDepartureTime(T00_02)
      .searchWindowInSeconds(D1m)
      .addAccessPaths(TestAccessEgress.walk(STOP_A, D1m))
      .addEgressPaths(TestAccessEgress.walk(STOP_B, D1m))
      .build();

    var result = raptorService.findAllDirectTransit(request, data);

    assertEquals(
      "Walk 1m ~ A ~ BUS R1 0:03 0:04 ~ B ~ Walk 1m [0:02 0:05 3m Tₙ0 C₁900]\n" +
        "Walk 1m ~ A ~ BUS R1 0:04 0:05 ~ B ~ Walk 1m [0:03 0:06 3m Tₙ0 C₁900]",
      pathsToString(result)
    );
  }
}
