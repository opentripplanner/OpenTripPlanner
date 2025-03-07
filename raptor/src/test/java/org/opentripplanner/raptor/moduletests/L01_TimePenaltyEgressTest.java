package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.api.PathUtils.withoutCost;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.walk;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_STANDARD;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_STANDARD_ONE;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_STANDARD_REV;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_STANDARD_REV_ONE;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.multiCriteria;
import static org.opentripplanner.utils.time.TimeUtils.time;

import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.api.PathUtils;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.raptor.moduletests.support.ExpectedList;
import org.opentripplanner.raptor.moduletests.support.ModuleTestDebugLogging;
import org.opentripplanner.raptor.moduletests.support.RaptorModuleTestCase;

/*
 * FEATURE UNDER TEST
 *
 * Raptor should take into account time-penalty for egress. This test focuses on checking the
 * logic for the time-penalty. The penalty should be included in the egress when comparing for
 * optimality, but should be excluded when checking for time constraints (arrive-by/depart-after).
 * All paths in this test have the same penalty; Hence, we do not compare paths with/without a
 * penalty.
 * <p>
 * Tip! Remove time-penalty from egress and the test should in most cases have the same result.
 */
public class L01_TimePenaltyEgressTest implements RaptorTestConstants {

  private static final Duration D8m = Duration.ofMinutes(8);

  // There are 5 possible trips

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder =
    new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );

  @BeforeEach
  public void setup() {
    data.withRoute(route("R1", STOP_A, STOP_B).withTimetable(schedule("0:10 0:40").repeat(10, 60)));
    requestBuilder
      .searchParams()
      .addAccessPaths(walk(STOP_A, D1m))
      .addEgressPaths(walk(STOP_B, D2m).withTimePenalty(D1m));

    requestBuilder.searchParams().timetable(true);

    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  private static List<RaptorModuleTestCase> firstTwoPathsArriveBeforeLAT() {
    // EDT is set to allow 5 paths - not a limiting factor.
    int edt = time("0:05");
    // We limit the search to the two first paths by setting the LAT to 0:43.
    int lat = time("0:43");

    var expected = new ExpectedList(
      "BUS R1 0:10 0:40 30m ~ B 0s ~ Walk 2m 0:40 0:42 [0:09 0:42 33m Tₓ0]",
      "BUS R1 0:11 0:41 30m ~ B 0s ~ Walk 2m 0:41 0:43 [0:10 0:43 33m Tₓ0]"
    );

    return RaptorModuleTestCase.of()
      .withRequest(r ->
        r.searchParams().earliestDepartureTime(edt).latestArrivalTime(lat).searchWindow(D8m)
      )
      .addMinDuration("34m", TX_0, edt, lat)
      .add(TC_STANDARD, withoutCost(expected.all()))
      .add(TC_STANDARD_ONE, withoutCost(expected.first()))
      .add(TC_STANDARD_REV, withoutCost(expected.all()))
      // The egress time-penalty will cause the first path to be missed (singe iteration)
      .add(TC_STANDARD_REV_ONE, withoutCost(expected.first()))
      .add(multiCriteria(), expected.all())
      .build();
  }

  @ParameterizedTest
  @MethodSource("firstTwoPathsArriveBeforeLAT")
  void firstTwoPathsArriveBeforeLAT(RaptorModuleTestCase testCase) {
    assertEquals(
      testCase.expected(),
      focusOnEgress(testCase.runDetailedResult(raptorService, data, requestBuilder))
    );
  }

  private static List<RaptorModuleTestCase> lastTwoPathsDepartsAfterEDT() {
    // The latest buss is at 0:19, so with EDT=0:17 can only reach the last two buses.
    int edt = time("0:17");
    int lat = time("0:51");

    var expected = new ExpectedList(
      "BUS R1 0:18 0:48 30m ~ B 0s ~ Walk 2m 0:48 0:50 [0:17 0:50 33m Tₓ0]",
      "BUS R1 0:19 0:49 30m ~ B 0s ~ Walk 2m 0:49 0:51 [0:18 0:51 33m Tₓ0]"
    );

    return RaptorModuleTestCase.of()
      .withRequest(r ->
        r.searchParams().earliestDepartureTime(edt).latestArrivalTime(lat).searchWindow(D8m)
      )
      .addMinDuration("34m", TX_0, edt, lat)
      // Note! this test that the time-penalty is removed from the "arrive-by" limit in the
      // destination
      .add(TC_STANDARD, withoutCost(expected.all()))
      .add(TC_STANDARD_ONE, withoutCost(expected.first()))
      .add(TC_STANDARD_REV, withoutCost(expected.all()))
      // We do not have special support for time-penalty for single iteration Raptor, so the
      // "last" path is missed due to the penalty for a reverse search.
      .add(TC_STANDARD_REV_ONE, withoutCost(expected.first()))
      .add(multiCriteria(), expected.all())
      .build();
  }

  @ParameterizedTest
  @MethodSource("lastTwoPathsDepartsAfterEDT")
  void lastTwoPathsDepartsAfterEDT(RaptorModuleTestCase testCase) {
    assertEquals(
      testCase.expected(),
      focusOnEgress(testCase.runDetailedResult(raptorService, data, requestBuilder))
    );
  }

  public static String focusOnEgress(String path) {
    // We are only interested in the access and the first boarding. We include the
    // pareto vector as well.
    var p = Pattern.compile("(BUS R1 .+)(\\[.+)");

    // BUS R1 0:18 0:48 30m ~ B 0s ~ Walk 1m 0:48 0:49  .. [0:16 0:49 33m Tₓ0]
    String[] lines = path.split("\n");
    return Stream.of(lines)
      .map(s -> {
        int pos = s.indexOf("BUS");
        return pos > 0 ? s.substring(pos) : s;
      })
      .map(PathUtils::withoutCost)
      .collect(Collectors.joining("\n"));
  }
}
