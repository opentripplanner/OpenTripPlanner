package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.utils.time.TimeUtils.hm2time;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.configure.RaptorTestFactory;
import org.opentripplanner.raptor.extensions.direct.api.RaptorDirectTransitRequest;

/**
 * FEATURE UNDER TEST
 * <p>
 * The direct transit search should not return paths arriving after the latest-arrival-time. The
 * latest-arrival-time includes the egress.
 */
public class M06_DirectTransitLatestArrivalTimeTest implements RaptorTestConstants {

  private static final int T00_08 = hm2time(0, 8);

  private final TestTransitData data = new TestTransitData();
  private final RaptorService<TestTripSchedule> raptorService = RaptorTestFactory.raptorService();

  @BeforeEach
  void setup() {
    data
      .withTimetable(
        "R1",
        """
        A     B
        00:02 00:05
        00:04 00:07
        """
      )
      .withTimetable(
        "R2",
        """
        A     B
        00:05 00:09
        """
      );
  }

  @Test
  void testLatestArrivalTime() {
    var request = RaptorDirectTransitRequest.of()
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T00_08)
      .searchWindowInSeconds(D10_m)
      .addAccessPaths(TestAccessEgress.walk(STOP_A, D1_m))
      .addEgressPaths(TestAccessEgress.walk(STOP_B, D1_m))
      .build();

    var result = raptorService.findAllDirectTransit(request, data);

    assertEquals(
      "Walk 1m ~ A ~ BUS R1 0:02 0:05 ~ B ~ Walk 1m [0:01 0:06 5m Tₙ0 C₁1_020]\n" +
        "Walk 1m ~ A ~ BUS R1 0:04 0:07 ~ B ~ Walk 1m [0:03 0:08 5m Tₙ0 C₁1_020]",
      pathsToString(result)
    );
  }

  @Test
  void testLatestArrivalTimeNotSet() {
    var request = RaptorDirectTransitRequest.of()
      .earliestDepartureTime(T00_00)
      .searchWindowInSeconds(D10_m)
      .addAccessPaths(TestAccessEgress.walk(STOP_A, D1_m))
      .addEgressPaths(TestAccessEgress.walk(STOP_B, D1_m))
      .build();

    var result = raptorService.findAllDirectTransit(request, data);

    assertEquals(
      "Walk 1m ~ A ~ BUS R1 0:02 0:05 ~ B ~ Walk 1m [0:01 0:06 5m Tₙ0 C₁1_020]\n" +
        "Walk 1m ~ A ~ BUS R1 0:04 0:07 ~ B ~ Walk 1m [0:03 0:08 5m Tₙ0 C₁1_020]\n" +
        "Walk 1m ~ A ~ BUS R2 0:05 0:09 ~ B ~ Walk 1m [0:04 0:10 6m Tₙ0 C₁1_080]",
      pathsToString(result)
    );
  }
}
