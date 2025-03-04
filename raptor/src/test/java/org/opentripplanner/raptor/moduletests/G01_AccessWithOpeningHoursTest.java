package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.api.PathUtils.withoutCost;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.free;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.walk;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_STANDARD;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_STANDARD_ONE;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_STANDARD_REV;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_STANDARD_REV_ONE;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.minDuration;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.multiCriteria;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.standard;

import java.time.Duration;
import java.util.List;
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
import org.opentripplanner.raptor.moduletests.support.RaptorModuleTestCaseFactory;
import org.opentripplanner.utils.time.TimeUtils;

/*
 * FEATURE UNDER TEST
 *
 * Raptor should take into account opening hours/time restrictions on access. If the time
 * restrictions require it, there should be a wait before boarding the trip so that the access is
 * traversed while "open".
 */
public class G01_AccessWithOpeningHoursTest implements RaptorTestConstants {

  private static final int T00_18 = TimeUtils.time("00:18");
  private static final int T00_20 = TimeUtils.time("00:20");
  private static final int T00_23 = TimeUtils.time("00:23");
  private static final int T24_10 = TimeUtils.time("24:10");
  private static final int T24_40 = TimeUtils.time("24:40");
  private static final int T25_00 = TimeUtils.time("25:00");

  // There are 5 possible trips
  private static final String EXP_00_15 = " ~ B ~ BUS R1 0:15 0:30 ~ E ~ Walk 1m ";
  private static final String EXP_00_20 = " ~ B ~ BUS R1 0:20 0:35 ~ E ~ Walk 1m ";
  private static final String EXP_00_25 = " ~ B ~ BUS R1 0:25 0:40 ~ E ~ Walk 1m ";
  private static final String EXP_00_30 = " ~ B ~ BUS R1 0:30 0:45 ~ E ~ Walk 1m ";
  private static final String EXP_24_15 = " ~ B ~ BUS R1 0:15+1d 0:30+1d ~ E ~ Walk 1m ";
  private static final String EXP_24_20 = " ~ B ~ BUS R1 0:20+1d 0:35+1d ~ E ~ Walk 1m ";
  private static final String EXP_24_25 = " ~ B ~ BUS R1 0:25+1d 0:40+1d ~ E ~ Walk 1m ";

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder =
    new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );

  @BeforeEach
  public void setup() {
    data.withRoute(
      route("R1", STOP_B, STOP_E).withTimetable(
        schedule("00:15 00:30"),
        schedule("00:20 00:35"),
        schedule("00:25 00:40"),
        schedule("00:30 00:45"),
        schedule("24:15 24:30"),
        schedule("24:20 24:35"),
        // Not within time-limit 24:42 (need 2 min for egress)
        schedule("24:25 24:40")
      )
    );
    requestBuilder.searchParams().addEgressPaths(walk(STOP_E, D1m));

    requestBuilder
      .searchParams()
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T24_40)
      .searchWindow(Duration.ofMinutes(30))
      .timetable(true);

    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  private static List<RaptorModuleTestCase> openInSearchIntervalCases(String access) {
    var expected = new ExpectedList(
      access + EXP_00_15 + "[0:13 0:31 18m Tₓ0 C₁1_860]",
      access + EXP_00_20 + "[0:18 0:36 18m Tₓ0 C₁1_860]",
      access + EXP_00_25 + "[0:23 0:41 18m Tₓ0 C₁1_860]",
      access + EXP_00_30 + "[0:28 0:46 18m Tₓ0 C₁1_860]",
      access + EXP_24_15 + "[0:13+1d 0:31+1d 18m Tₓ0 C₁1_860]",
      access + EXP_24_20 + "[0:18+1d 0:36+1d 18m Tₓ0 C₁1_860]"
    );

    return tcBuilderWithMinDuration(T00_00, T24_40)
      // Should find all 4 on current day and the first next day (within search-window)
      .add(TC_STANDARD, withoutCost(expected.first(5)))
      .add(TC_STANDARD_ONE, withoutCost(expected.first()))
      // Should find 2 on next day and the last one today (within search-window)
      .add(TC_STANDARD_REV, withoutCost(expected.last(3)))
      .add(TC_STANDARD_REV_ONE, withoutCost(expected.last()))
      .add(multiCriteria(), expected.first(5))
      .build();
  }

  static List<RaptorModuleTestCase> openAllDayTestCases() {
    return openInSearchIntervalCases("Walk 2m");
  }

  /*
   * There is no time restriction, all routes are found.
   */
  @ParameterizedTest
  @MethodSource("openAllDayTestCases")
  void openAllDayTest(RaptorModuleTestCase testCase) {
    requestBuilder.searchParams().addAccessPaths(walk(STOP_B, D2m));
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }

  private static List<RaptorModuleTestCase> openInSearchIntervalTestCases() {
    return openInSearchIntervalCases("Walk 2m Open(0:00 1:00)");
  }

  @ParameterizedTest
  @MethodSource("openInSearchIntervalTestCases")
  public void openInSearchIntervalTest(RaptorModuleTestCase testCase) {
    requestBuilder.searchParams().addAccessPaths(walk(STOP_B, D2m).openingHours(T00_00, T01_00));
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }

  private static List<RaptorModuleTestCase> openInSearchIntervalStartSearchNextDayTestCase() {
    String access = "Walk 2m Open(0:00 1:00)";
    var expected = new ExpectedList(
      access + EXP_24_15 + "[0:13+1d 0:31+1d 18m Tₓ0 C₁1_860]",
      access + EXP_24_20 + "[0:18+1d 0:36+1d 18m Tₓ0 C₁1_860]"
    );

    return tcBuilderWithMinDuration(T24_10, T24_40)
      .withRequest(r ->
        r
          .searchParams()
          .earliestDepartureTime(T24_10)
          .addAccessPaths(walk(STOP_B, D2m).openingHours(T00_00, T01_00))
      )
      .add(standard().manyIterations(), withoutCost(expected.all()))
      .add(TC_STANDARD_ONE, withoutCost(expected.first()))
      .add(TC_STANDARD_REV_ONE, withoutCost(expected.last()))
      .add(multiCriteria(), expected.all())
      .build();
  }

  @ParameterizedTest
  @MethodSource("openInSearchIntervalStartSearchNextDayTestCase")
  public void openInSearchIntervalStartSearchNextDayTest(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }

  private static List<RaptorModuleTestCase> openInSecondHalfTodayTestCase() {
    String access = "Walk 2m Open(0:23 1:00)";
    var expected = new ExpectedList(
      access + EXP_00_25 + "[0:23 0:41 18m Tₓ0 C₁1_860]",
      access + EXP_00_30 + "[0:28 0:46 18m Tₓ0 C₁1_860]",
      access + EXP_24_15 + "[1:00 0:31+1d 23h31m Tₓ0 C₁85_440]",
      access + EXP_24_20 + "[1:00 0:36+1d 23h36m Tₓ0 C₁0_000]"
    );

    return tcBuilderWithMinDuration(T00_00, T24_40)
      .withRequest(r ->
        r
          .searchParams()
          .searchWindow(Duration.ofHours(1))
          .addAccessPaths(walk(STOP_B, D2m).openingHours(T00_23, T01_00))
      )
      .add(TC_STANDARD, withoutCost(expected.first(3)))
      .add(TC_STANDARD_ONE, withoutCost(expected.first()))
      .add(TC_STANDARD_REV, withoutCost(expected.range(1, 3)))
      .add(TC_STANDARD_REV_ONE, withoutCost(expected.last()))
      .add(multiCriteria(), expected.first(3))
      .build();
  }

  @ParameterizedTest
  @MethodSource("openInSecondHalfTodayTestCase")
  public void openInSecondHalfTodayTest(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }

  private static List<RaptorModuleTestCase> openInFirstHalfIntervalTestCase() {
    String access = "Walk 2m Open(0:00 0:20)";
    var expected = new ExpectedList(
      access + EXP_00_15 + "[0:13 0:31 18m Tₓ0 C₁1_860]",
      access + EXP_00_20 + "[0:18 0:36 18m Tₓ0 C₁1_860]",
      access + EXP_00_25 + "[0:20 0:41 21m Tₓ0 C₁2_040]",
      access + EXP_00_30 + "[0:20 0:46 26m Tₓ0 C₁0_000]",
      access + EXP_24_15 + "[0:13+1d 0:31+1d 18m Tₓ0 C₁1_860]",
      access + EXP_24_20 + "[0:18+1d 0:36+1d 18m Tₓ0 C₁1_860]"
    );

    return tcBuilderWithMinDuration(T00_00, T24_40)
      .withRequest(r ->
        r
          .searchParams()
          .searchWindow(Duration.ofHours(1))
          .addAccessPaths(walk(STOP_B, D2m).openingHours(T00_00, T00_20))
      )
      .add(TC_STANDARD, withoutCost(expected.get(0, 1, 2, 4)))
      .add(TC_STANDARD_ONE, withoutCost(expected.first()))
      .add(TC_STANDARD_REV, withoutCost(expected.range(3, 6)))
      .add(TC_STANDARD_REV_ONE, withoutCost(expected.last()))
      .add(multiCriteria(), expected.get(0, 1, 2, 4))
      .build();
  }

  /*
   * The access is only open before 00:20, which means that we arrive at the stop
   * at 00:22 at the latest.
   */
  @ParameterizedTest
  @MethodSource("openInFirstHalfIntervalTestCase")
  public void openInFirstHalfIntervalTest(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }

  private static List<RaptorModuleTestCase> partiallyOpenIntervalTestNextDayTestCase() {
    String access = "Walk 2m Open(0:18 0:20)";
    var expected = new ExpectedList(
      access + EXP_24_20 + "[0:18+1d 0:36+1d 18m Tₓ0 C₁1_860]",
      access + EXP_24_25 + "[0:20+1d 0:41+1d 21m Tₓ0 C₁2_040]"
    );

    return tcBuilderWithMinDuration(T24_10, T25_00)
      .withRequest(r ->
        r
          .searchParams()
          .searchWindow(Duration.ofMinutes(30))
          .earliestDepartureTime(T24_10)
          .latestArrivalTime(T25_00)
          .addAccessPaths(walk(STOP_B, D2m).openingHours(T00_18, T00_20))
      )
      .add(TC_STANDARD, withoutCost(expected.all()))
      .add(TC_STANDARD_ONE, withoutCost(expected.first()))
      .add(TC_STANDARD_REV, withoutCost(expected.all()))
      .add(TC_STANDARD_REV_ONE, withoutCost(expected.last()))
      .add(multiCriteria(), expected.all())
      .build();
  }

  /*
   * The access is only open from 00:18 to 00:20, which means that we arrive at
   * the stop at 24:20 .. 24:22.
   */
  @ParameterizedTest
  @MethodSource("partiallyOpenIntervalTestNextDayTestCase")
  public void partiallyOpenIntervalTestNextDayTest(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }

  private static List<RaptorModuleTestCase> closedTestCase() {
    return RaptorModuleTestCase.of()
      .withRequest(r ->
        r
          .searchParams()
          .searchWindow(Duration.ofHours(2))
          .addAccessPaths(free(STOP_B).openingHoursClosed())
      )
      .add(minDuration())
      .add(standard())
      .add(multiCriteria())
      .build();
  }

  /*
   * The access is only open from 00:18 to 00:20, which means that we arrive at
   * the stop at 24:20 .. 24:22.
   */
  @ParameterizedTest
  @MethodSource("closedTestCase")
  public void closedTest(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }

  private static RaptorModuleTestCaseFactory tcBuilderWithMinDuration(
    int earliestDepartureTime,
    int latestArrivalTime
  ) {
    return RaptorModuleTestCase.of()
      .addMinDuration("18m", TX_0, earliestDepartureTime, latestArrivalTime);
  }
}
