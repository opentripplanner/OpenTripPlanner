package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.multiCriteria;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.standard;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.api.PathUtils;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorTestFactory;
import org.opentripplanner.raptor.moduletests.support.RaptorModuleTestCase;

///
/// In this test boarding a pattern at stop 1 is the only valid path to the destination via
/// stop 2. But the pattern can be reached in earlier rounds at later stops. The point is to
/// show that early round pattern arrivals does not dominate boarding early in the pattern.
///
/// **Network**
/// ```
///          (Destination)         R1
///     E——————————F——————————G——————————H——————————I——————————J
///     ⎮                     ⎮          ⎮          ⎮
///     ⎮ R8                  ⎮ R6       ⎮ R4       ⎮ R2
///     ⎮                     ⎮          ⎮          ⎮
///     D ——————————————————— C ———————— B ———————— A (Origin)
///                R7              R5         R3
/// ```
/// **Origin:** Stop A
/// **Destination:** Stop F
/// **Routes:**
///    - R1 : E - F - G - H - I - J
///    - R2 : A - I
///    - R3 : A - B
///    - R4 : B - H
///    - R5 : B - C
///    - R6 : C - G
///    - R7 : C - D
///    - R8 : D - E
///
/// **Only path to destination:** A ~ R3 ~ B ~ R5 ~ C ~ R7 ~ D ~ R8 ~ E ~ R1 ~ F
public class D02_EarlyBoardingMatters implements RaptorTestConstants {

  private static final int TX_4 = 4;

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = data.requestBuilder();
  private final RaptorService<TestTripSchedule> raptorService = RaptorTestFactory.raptorService();

  @BeforeEach
  void setup() {
    data
      .access("Free ~ A")
      .withTimetables(
        """
        -- R1
        E      F      G      H      I      J
        00:02  00:04  00:06  00:08  00:10  00:12
        00:04  00:06  00:08  00:10  00:12  00:14
        00:06  00:08  00:10  00:12  00:14  00:16
        00:08  00:10  00:12  00:14  00:16  00:18
        00:10  00:12  00:14  00:16  00:18  00:20
        00:12  00:14  00:16  00:18  00:20  00:22
        00:14  00:16  00:18  00:20  00:22  00:24
        -- R2
        A      I
        00:03  00:05
        -- R3
        A      B
        00:02  00:04
        -- R4
        B      H
        00:06  00:08
        -- R5
        B      C
        00:05  00:07
        -- R6
        C      G
        00:09  00:11
        -- R7
        C      D
        00:08  00:10
        -- R8
        D      E
        00:11  00:13
        """
      )
      .egress("F ~ Free");

    requestBuilder
      .searchParams()
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T01_00)
      .searchWindow(Duration.ofMinutes(10))
      .timetable(true);
  }

  static List<RaptorModuleTestCase> testCases() {
    var path =
      "A ~ BUS R3 0:02 0:04 ~ " +
      "B ~ BUS R5 0:05 0:07 ~ " +
      "C ~ BUS R7 0:08 0:10 ~ " +
      "D ~ BUS R8 0:11 0:13 ~ " +
      "E ~ BUS R1 0:14 0:16 ~ " +
      "F [0:02 0:16 14m Tₙ4 C₁3_840]";
    return RaptorModuleTestCase.of()
      .addMinDuration("14m", TX_4, T00_00, T01_00)
      .add(standard(), PathUtils.withoutCost(path))
      .add(multiCriteria(), path)
      .build();
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void testRaptor(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }
}
