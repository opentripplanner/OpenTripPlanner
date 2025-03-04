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
 * Raptor should take into account time-penalty for access. This test focuses on checking the
 * logic for the time-penalty. The penalty should be included in the access when comparing for
 * optimality, but should be excluded when checking for time constraints (arrive-by/depart-after).
 * All paths in this test have the same penalty; Hence, we do not compare paths with/without a
 * penalty.
 */
public class L01_TimePenaltyAccessTest implements RaptorTestConstants {

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
      .addAccessPaths(walk(STOP_A, D2m).withTimePenalty(D1m))
      .addEgressPaths(walk(STOP_B, D1m));

    requestBuilder.searchParams().timetable(true);

    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  private static List<RaptorModuleTestCase> tripsAtTheEndOfTheSearchWindowTestCase() {
    int edt = time("0:01");
    int lat = time("0:42");

    var expected = new ExpectedList(
      "Walk 2m 0:08 0:10 C₁240 ~ A 0s ~ BUS R1 0:10 .. [0:08 0:41 33m Tₓ0 C₁2_760]",
      "Walk 2m 0:09 0:11 C₁240 ~ A 0s ~ BUS R1 0:11 .. [0:09 0:42 33m Tₓ0 C₁2_760]"
    );

    return RaptorModuleTestCase.of()
      .withRequest(r ->
        r.searchParams().earliestDepartureTime(edt).latestArrivalTime(lat).searchWindow(D8m)
      )
      .addMinDuration("34m", TX_0, edt, lat)
      .add(TC_STANDARD, withoutCost(expected.all()))
      .add(TC_STANDARD_ONE, withoutCost(expected.first()))
      .add(TC_STANDARD_REV, withoutCost(expected.all()))
      .add(TC_STANDARD_REV_ONE, withoutCost(expected.last()))
      .add(multiCriteria(), expected.all())
      .build();
  }

  @ParameterizedTest
  @MethodSource("tripsAtTheEndOfTheSearchWindowTestCase")
  void tripsAtTheEndOfTheSearchWindowTest(RaptorModuleTestCase testCase) {
    assertEquals(
      testCase.expected(),
      focusOnAccess(testCase.runDetailedResult(raptorService, data, requestBuilder))
    );
  }

  private static List<RaptorModuleTestCase> tripsAtTheBeginningOfTheSearchWindowTestCases() {
    int edt = time("0:16");
    // The last path arrive at the destination at 0:50, LAT=0:55 will iterate over the last 5
    // paths.
    int lat = time("0:55");

    // The latest buss is at 0:19, so with EDT=0:16 can only reach the last two buses,
    // Running this test without the time-penalty confirm this result.
    var expected = new ExpectedList(
      "Walk 2m 0:16 0:18 C₁240 ~ A 0s ~ BUS R1 0:18 .. [0:16 0:49 33m Tₓ0 C₁2_760]",
      "Walk 2m 0:17 0:19 C₁240 ~ A 0s ~ BUS R1 0:19 .. [0:17 0:50 33m Tₓ0 C₁2_760]"
    );

    return RaptorModuleTestCase.of()
      .withRequest(r ->
        r.searchParams().earliestDepartureTime(edt).latestArrivalTime(lat).searchWindow(D8m)
      )
      .addMinDuration("34m", TX_0, edt, lat)
      .add(TC_STANDARD, withoutCost(expected.all()))
      // We do not have special support for time-penalty for single iteration Raptor, so the
      // first path is missed due to the penalty.
      .add(TC_STANDARD_ONE, withoutCost(expected.last()))
      // Note! this test that the time-penalty is removed from the "arrive-by" limit in the
      // destination
      .add(TC_STANDARD_REV, withoutCost(expected.all()))
      .add(TC_STANDARD_REV_ONE, withoutCost(expected.last()))
      .add(multiCriteria(), expected.all())
      .build();
  }

  @ParameterizedTest
  @MethodSource("tripsAtTheBeginningOfTheSearchWindowTestCases")
  void tripsAtTheBeginningOfTheSearchWindowTest(RaptorModuleTestCase testCase) {
    assertEquals(
      testCase.expected(),
      focusOnAccess(testCase.runDetailedResult(raptorService, data, requestBuilder))
    );
  }

  public static String focusOnAccess(String path) {
    // We are only interested in the access and the first boarding. We include the
    // pareto vector as well.
    var p = Pattern.compile("(.+BUS R1 \\d+:\\d+).+(\\[.+)");

    String[] lines = path.split("\n");
    return Stream.of(lines)
      .map(s -> {
        var m = p.matcher(s);
        return m.find() ? m.group(1) + " .. " + m.group(2) : s;
      })
      .collect(Collectors.joining("\n"));
  }
}
