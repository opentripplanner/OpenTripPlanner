package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_STANDARD;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_STANDARD_ONE;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_STANDARD_REV;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_STANDARD_REV_ONE;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.multiCriteria;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorTestFactory;
import org.opentripplanner.raptor.moduletests.support.RaptorModuleTestCase;

/**
 * FEATURE UNDER TEST
 * <p>
 * An arrive-by search without timetable view, where we prefer the latest departure arriving before
 * the latest-arrival-time ({@code preferLateArrival}, also known as {@code departAsLateAsPossible}).
 * The destination pareto-set must prefer the latest departure (and for multi-criteria the shorter
 * duration), not the latest arrival. The latest-arrival-time must be enforced for every egress.
 * <p>
 * The expected results of this test are the same as before the destination pareto-set was changed
 * to use destination arrivals, they were verified by running the test on the previous version of
 * Raptor. The test is a guard against changing the criteria by mistake.
 * <p>
 * The data is the same as in {@link B03_AccessEgressTest}, with a second trip, so that the latest
 * departure matters. The last trip arrives at {@code H} at 1:00, and can not be used since
 * the egress ({@code 1s}) arrives after the latest-arrival-time.
 */
public class A05_ArriveByTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = data.requestBuilder();
  private final RaptorService<TestTripSchedule> raptorService = RaptorTestFactory.raptorService();

  @BeforeEach
  void setup() {
    data
      .access(
        // Lowest cost
        "Walk 1s ~ A",
        // Best compromise of cost and time
        "Walk 4m ~ B",
        // Latest departure time
        "Walk 7m ~ C",
        // Not optimal
        "Walk 20m ~ D"
      )
      .withTimetables(
        """
          A     B     C     D     E     F     G     H
          0:10  0:14  0:18  0:22  0:28  0:32  0:36  0:40
          0:30  0:34  0:38  0:42  0:48  0:52  0:56  1:00
        """
      )
      .egress("E ~ Walk 20m", "F ~ Walk 7m", "G ~ Walk 4m", "H ~ Walk 1s");

    requestBuilder
      .searchParams()
      .latestArrivalTime(T01_00)
      .timetable(false)
      .preferLateArrival(true);
  }

  /** The earliest departure time is not set, the Raptor search window is calculated. */
  static List<RaptorModuleTestCase> testCases() {
    return RaptorModuleTestCase.of()
      // Forward search: the latest departure
      .add(TC_STANDARD, "Walk 7m ~ C ~ BUS R1 0:38 0:52 ~ F ~ Walk 7m [0:31 0:59 28m Tₙ0]")
      // Reverse search: time flows backwards, the earliest arrival
      .add(TC_STANDARD_REV, "Walk 7m ~ C ~ BUS R1 0:18 0:32 ~ F ~ Walk 7m [0:11 0:39 28m Tₙ0]")
      .add(TC_STANDARD_REV_ONE, "Walk 7m ~ C ~ BUS R1 0:38 0:56 ~ G ~ Walk 4m [0:31 1:00 29m Tₙ0]")
      .add(
        multiCriteria(),
        "Walk 1s ~ A ~ BUS R1 0:10 0:40 ~ H ~ Walk 1s [0:09:59 0:40:01 30m2s Tₙ0 C₁2_404]",
        "Walk 7m ~ C ~ BUS R1 0:38 0:52 ~ F ~ Walk 7m [0:31 0:59 28m Tₙ0 C₁3_120]",
        "Walk 1s ~ A ~ BUS R1 0:30 0:52 ~ F ~ Walk 7m [0:29:59 0:59 29m1s Tₙ0 C₁2_762]",
        "Walk 7m ~ C ~ BUS R1 0:38 0:56 ~ G ~ Walk 4m [0:31 1:00 29m Tₙ0 C₁3_000]",
        "Walk 4m ~ B ~ BUS R1 0:34 0:56 ~ G ~ Walk 4m [0:30 1:00 30m Tₙ0 C₁2_880]",
        "Walk 1s ~ A ~ BUS R1 0:30 0:56 ~ G ~ Walk 4m [0:29:59 1:00 30m1s Tₙ0 C₁2_642]"
      )
      .build();
  }

  /** A forward search with one iteration needs the earliest departure time. */
  static List<RaptorModuleTestCase> testCasesWithEarliestDepartureTime() {
    return RaptorModuleTestCase.of()
      .withRequest(r -> r.searchParams().earliestDepartureTime(T00_00))
      .add(TC_STANDARD_ONE, "Walk 1s ~ A ~ BUS R1 0:10 0:32 ~ F ~ Walk 7m [0:09:59 0:39 29m1s Tₙ0]")
      .build();
  }

  static Stream<RaptorModuleTestCase> allTestCases() {
    return Stream.concat(testCases().stream(), testCasesWithEarliestDepartureTime().stream());
  }

  @ParameterizedTest
  @MethodSource("allTestCases")
  void testRaptor(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }
}
