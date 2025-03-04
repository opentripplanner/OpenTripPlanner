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
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.minDuration;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.multiCriteria;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.standard;
import static org.opentripplanner.utils.time.TimeUtils.hm2time;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
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
 * Raptor should take into account time restrictions on egress. If the time restrictions require it,
 * there should be a wait before boarding the egress so that the egress is traversed while "open".
 */
public class G02_EgressWithOpeningHoursTest implements RaptorTestConstants {

  private static final int T00_25 = hm2time(0, 25);
  private static final int T00_35 = hm2time(0, 35);
  private static final int T24_10 = hm2time(24, 10);
  private static final int T25_00 = hm2time(25, 0);
  private static final Duration D15m = Duration.ofMinutes(15);
  private static final Duration D25h = Duration.ofHours(25);

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder =
    new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );

  @BeforeEach
  public void setup() {
    data.withRoute(
      route("R1", STOP_A, STOP_B).withTimetable(
        schedule("00:10 00:20"),
        schedule("00:20 00:30"),
        schedule("00:30 00:40"),
        schedule("24:20 24:30")
      )
    );
    requestBuilder.searchParams().addAccessPaths(TestAccessEgress.free(STOP_A));

    requestBuilder
      .searchParams()
      .earliestDepartureTime(T00_10)
      .latestArrivalTime(T25_00)
      .searchWindow(D15m)
      .timetable(true);

    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  private static List<RaptorModuleTestCase> openNoTimeRestrictionTestCase() {
    var expected = new ExpectedList(
      "A ~ BUS R1 0:10 0:20 ~ B ~ Walk 2m [0:10 0:22 12m Tₓ0 C₁1_440]",
      "A ~ BUS R1 0:20 0:30 ~ B ~ Walk 2m [0:20 0:32 12m Tₓ0 C₁1_440]",
      "A ~ BUS R1 0:30 0:40 ~ B ~ Walk 2m [0:30 0:42 12m Tₓ0 C₁1_440]",
      "A ~ BUS R1 0:20+1d 0:30+1d ~ B ~ Walk 2m [0:20+1d 0:32+1d 12m Tₓ0]"
    );

    return RaptorModuleTestCase.of()
      .withRequest(r -> r.searchParams().addEgressPaths(walk(STOP_B, D2m)))
      .addMinDuration("12m", TX_0, T00_10, T25_00)
      .add(TC_STANDARD, withoutCost(expected.first(3)))
      .add(TC_STANDARD_ONE, withoutCost(expected.first()))
      .add(TC_STANDARD_REV, withoutCost(expected.last()))
      .add(TC_STANDARD_REV_ONE, withoutCost(expected.last()))
      .add(multiCriteria(), expected.first(3))
      .build();
  }

  /*
   * There is no time restriction, all routes are found.
   */
  @ParameterizedTest
  @MethodSource("openNoTimeRestrictionTestCase")
  void openNoTimeRestrictionTest(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }

  private static List<RaptorModuleTestCase> openOneHourTestCase() {
    var expected = new ExpectedList(
      "A ~ BUS R1 0:10 0:20 ~ B ~ Walk 2m Open(0:00 1:00) [0:10 0:22 12m Tₓ0 C₁1_440]",
      "A ~ BUS R1 0:20 0:30 ~ B ~ Walk 2m Open(0:00 1:00) [0:20 0:32 12m Tₓ0 C₁1_440]",
      "A ~ BUS R1 0:30 0:40 ~ B ~ Walk 2m Open(0:00 1:00) [0:30 0:42 12m Tₓ0 C₁1_440]",
      "A ~ BUS R1 0:20+1d 0:30+1d ~ B ~ Walk 2m Open(0:00 1:00) [0:20+1d 0:32+1d 12m Tₓ0 C₁1_440]"
    );

    return RaptorModuleTestCase.of()
      .withRequest(r ->
        r.searchParams().addEgressPaths(walk(STOP_B, D2m).openingHours(T00_00, T01_00))
      )
      .addMinDuration("12m", TX_0, T00_10, T25_00)
      .add(TC_STANDARD, withoutCost(expected.first(3)))
      .add(TC_STANDARD_ONE, withoutCost(expected.first()))
      .add(TC_STANDARD_REV, withoutCost(expected.last()))
      .add(TC_STANDARD_REV_ONE, withoutCost(expected.last()))
      .add(multiCriteria(), expected.first(3))
      .build();
  }

  @ParameterizedTest
  @MethodSource("openOneHourTestCase")
  void openOneHourTest(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }

  private static List<RaptorModuleTestCase> openInWholeSearchIntervalTestNextDayTestCase() {
    var expected =
      "A ~ BUS R1 0:20+1d 0:30+1d ~ B ~ Walk 2m Open(0:00 1:00) [0:20+1d 0:32+1d 12m Tₓ0 C₁1_440]";

    return RaptorModuleTestCase.of()
      .withRequest(r ->
        r
          .searchParams()
          .earliestDepartureTime(T24_10)
          .addEgressPaths(walk(STOP_B, D2m).openingHours(T00_00, T01_00))
      )
      .addMinDuration("12m", TX_0, T24_10, T25_00)
      .add(standard(), withoutCost(expected))
      .add(multiCriteria(), expected)
      .build();
  }

  @ParameterizedTest
  @MethodSource("openInWholeSearchIntervalTestNextDayTestCase")
  void openInWholeSearchIntervalTestNextDayTest(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }

  private static List<RaptorModuleTestCase> openInFirstHalfIntervalTestCase() {
    var expected = new ExpectedList(
      "A ~ BUS R1 0:10 0:20 ~ B ~ Walk 2m Open(0:00 0:25) [0:10 0:22 12m Tₓ0 C₁1_440]",
      "A ~ BUS R1 0:30 0:40 ~ B ~ Walk 2m Open(0:00 0:25) [0:30 0:02+1d 23h32m Tₓ0 C₁85_440]"
    );

    return RaptorModuleTestCase.of()
      .withRequest(r ->
        r.searchParams().addEgressPaths(walk(STOP_B, D2m).openingHours(T00_00, T00_25))
      )
      .addMinDuration("12m", TX_0, T00_10, T25_00)
      .add(TC_STANDARD, withoutCost(expected.all()))
      .add(TC_STANDARD_ONE, withoutCost(expected.first()))
      .add(TC_STANDARD_REV, withoutCost(expected.last()))
      .add(TC_STANDARD_REV_ONE, withoutCost(expected.last()))
      .add(multiCriteria(), expected.all())
      .build();
  }

  @ParameterizedTest
  @MethodSource("openInFirstHalfIntervalTestCase")
  void openInFirstHalfIntervalTest(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }

  private static List<RaptorModuleTestCase> openInFirstHalfIntervalTestNextDayTestCase() {
    var expected =
      "A ~ BUS R1 0:20+1d 0:30+1d ~ B ~ Walk 2m Open(0:25 0:40) [0:20+1d 0:32+1d 12m Tₓ0 C₁1_440]";

    return RaptorModuleTestCase.of()
      .withRequest(r ->
        r
          .searchParams()
          .earliestDepartureTime(T24_10)
          .latestArrivalTime(T25_00)
          .searchWindow(Duration.ofMinutes(30))
          .addEgressPaths(walk(STOP_B, D2m).openingHours(T00_25, T00_40))
      )
      .addMinDuration("12m", TX_0, T24_10, T25_00)
      .add(standard(), withoutCost(expected))
      .add(multiCriteria(), expected)
      .build();
  }

  @ParameterizedTest
  @MethodSource("openInFirstHalfIntervalTestNextDayTestCase")
  void openInFirstHalfIntervalTestNextDayTest(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }

  private static List<RaptorModuleTestCase> partiallyOpenIntervalTestCase() {
    var expected = new ExpectedList(
      "A ~ BUS R1 0:10 0:20 ~ B ~ Walk 2m Open(0:25 0:35) [0:10 0:27 17m Tₓ0 C₁1_740]",
      "A ~ BUS R1 0:20 0:30 ~ B ~ Walk 2m Open(0:25 0:35) [0:20 0:32 12m Tₓ0 C₁1_440]",
      "A ~ BUS R1 0:30 0:40 ~ B ~ Walk 2m Open(0:25 0:35) [0:30 0:27+1d 23h57m Tₓ0 C₁86_940]",
      "A ~ BUS R1 0:20+1d 0:30+1d ~ B ~ Walk 2m Open(0:25 0:35) [0:20+1d 0:32+1d 12m Tₓ0]"
    );

    return RaptorModuleTestCase.of()
      .withRequest(r ->
        r.searchParams().addEgressPaths(walk(STOP_B, D2m).openingHours(T00_25, T00_35))
      )
      .addMinDuration("12m", TX_0, T00_10, T25_00)
      .add(TC_STANDARD, withoutCost(expected.first(3)))
      .add(TC_STANDARD_ONE, withoutCost(expected.first()))
      .add(TC_STANDARD_REV, withoutCost(expected.last()))
      .add(TC_STANDARD_REV_ONE, withoutCost(expected.last()))
      .add(multiCriteria(), expected.first(3))
      .build();
  }

  /*
   * The access is only open after 00:18 and before 00:20. This means that we arrive at the stop at
   * 00:20 at the earliest and 00:22 at the latest.
   */
  @ParameterizedTest
  @MethodSource("partiallyOpenIntervalTestCase")
  void partiallyOpenIntervalTest(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }

  private static List<RaptorModuleTestCase> closedTestCase() {
    return RaptorModuleTestCase.of()
      .withRequest(r -> r.searchParams().addEgressPaths(walk(STOP_B, D2m).openingHoursClosed()))
      .add(minDuration())
      .add(standard())
      .add(multiCriteria())
      .build();
  }

  /*
   * The access is only open after 00:18 and before 00:20. This means that we arrive at the stop at
   * 00:20 at the earliest and 00:22 at the latest.
   */
  @ParameterizedTest
  @MethodSource("closedTestCase")
  void closedTest(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }
}
