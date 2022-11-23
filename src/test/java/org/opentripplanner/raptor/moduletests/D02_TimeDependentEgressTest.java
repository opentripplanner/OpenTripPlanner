package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.framework.time.TimeUtils.hm2time;
import static org.opentripplanner.raptor._data.api.PathUtils.join;
import static org.opentripplanner.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.raptor._data.api.PathUtils.pathsToStringDetailed;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.SECONDS_IN_DAY;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorProfile;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorConfig;

/*
 * FEATURE UNDER TEST
 *
 * Raptor should take into account time restrictions on egress. If the time restrictions require it,
 * there should be a wait before boarding the egress so that the egress is traversed while "open".
 */
public class D02_TimeDependentEgressTest implements RaptorTestConstants {

  private static final int T00_25 = hm2time(0, 25);
  private static final int T00_35 = hm2time(0, 35);
  private static final Duration D15m = Duration.ofMinutes(15);
  private static final Duration D25h = Duration.ofHours(25);

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );

  @BeforeEach
  public void setup() {
    data.withRoute(
      route("R1", STOP_A, STOP_B)
        .withTimetable(
          schedule("00:10 00:20"),
          schedule("00:20 00:30"),
          schedule("00:30 00:40"),
          schedule("24:20 24:30")
        )
    );
    requestBuilder.searchParams().addAccessPaths(TestAccessEgress.walk(STOP_A, D0s));

    requestBuilder
      .searchParams()
      .earliestDepartureTime(T00_10)
      .searchWindow(D15m)
      .timetableEnabled(true);

    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  /*
   * There is no time restriction, all routes are found.
   */
  @Test
  public void openInWholeSearchIntervalTest() {
    requestBuilder
      .profile(RaptorProfile.MULTI_CRITERIA)
      .searchParams()
      .addEgressPaths(TestAccessEgress.walk(STOP_B, D2m));

    // Note the last result is outside the search window
    assertEquals(
      join(
        "A ~ BUS R1 0:10 0:20 ~ B ~ Walk 2m [0:10 0:22 12m 0tx $1440]",
        "A ~ BUS R1 0:20 0:30 ~ B ~ Walk 2m [0:20 0:32 12m 0tx $1440]",
        "A ~ BUS R1 0:30 0:40 ~ B ~ Walk 2m [0:30 0:42 12m 0tx $1440]"
      ),
      runSearch()
    );
  }

  @Test
  public void openInWholeSearchIntervalTestFullDay() {
    requestBuilder
      .profile(RaptorProfile.MULTI_CRITERIA)
      .searchParams()
      .searchWindow(D25h)
      .addEgressPaths(TestAccessEgress.walk(STOP_B, D2m, T00_00, T01_00));

    assertEquals(
      join(
        "A ~ BUS R1 0:10 0:20 ~ B ~ Walk 2m [0:10 0:22 12m 0tx $1440]",
        "A ~ BUS R1 0:20 0:30 ~ B ~ Walk 2m [0:20 0:32 12m 0tx $1440]",
        "A ~ BUS R1 0:30 0:40 ~ B ~ Walk 2m [0:30 0:42 12m 0tx $1440]",
        "A ~ BUS R1 0:20+1d 0:30+1d ~ B ~ Walk 2m [0:20+1d 0:32+1d 12m 0tx $1440]"
      ),
      runSearch()
    );
  }

  @Test
  public void openInWholeSearchIntervalTestNextDay() {
    requestBuilder
      .profile(RaptorProfile.MULTI_CRITERIA)
      .searchParams()
      .earliestDepartureTime(SECONDS_IN_DAY + T00_10)
      .addEgressPaths(TestAccessEgress.walk(STOP_B, D2m, T00_00, T01_00));

    assertEquals(
      "A ~ BUS R1 0:20+1d 0:30+1d ~ B ~ Walk 2m [0:20+1d 0:32+1d 12m 0tx $1440]",
      runSearch()
    );
  }

  /*
   * The access is only open after 00:18, which means that we may arrive at the stop at 00:20 at
   * the earliest.
   */
  @Test
  public void openInSecondHalfIntervalTest() {
    requestBuilder
      .profile(RaptorProfile.MULTI_CRITERIA)
      .searchParams()
      .addEgressPaths(TestAccessEgress.walk(STOP_B, D2m, T00_35, T01_00));

    assertEquals(
      join(
        // The first egress is only available after waiting for 5 minutes
        "0:20 0:20A 0s ~ BUS R1 0:20 0:30 10m $1200 ~ B 5m ~ Walk 2m 0:35 0:37 $540 [0:20 0:37 17m 0tx $1740]",
        "0:30 0:30A 0s ~ BUS R1 0:30 0:40 10m $1200 ~ B 0s ~ Walk 2m 0:40 0:42 $240 [0:30 0:42 12m 0tx $1440]"
      ),
      runSearchDetailedResult()
    );
  }

  @Test
  public void openInSecondHalfIntervalTestFullDay() {
    requestBuilder
      .profile(RaptorProfile.MULTI_CRITERIA)
      .searchParams()
      .searchWindow(D25h)
      .addEgressPaths(TestAccessEgress.walk(STOP_B, D2m, T00_35, T01_00));

    // First and last path have time-shifted egress (wait 5 minutes to board)
    assertEquals(
      join(
        "A ~ BUS R1 0:20 0:30 ~ B ~ Walk 2m [0:20 0:37 17m 0tx $1740]",
        "A ~ BUS R1 0:30 0:40 ~ B ~ Walk 2m [0:30 0:42 12m 0tx $1440]",
        "A ~ BUS R1 0:20+1d 0:30+1d ~ B ~ Walk 2m [0:20+1d 0:37+1d 17m 0tx $1740]"
      ),
      runSearch()
    );
  }

  @Test
  public void openInSecondHalfIntervalTestNextDay() {
    requestBuilder
      .profile(RaptorProfile.MULTI_CRITERIA)
      .searchParams()
      .earliestDepartureTime(SECONDS_IN_DAY + T00_10)
      .addEgressPaths(TestAccessEgress.walk(STOP_B, D2m, T00_25, T01_00));

    assertEquals(
      "A ~ BUS R1 0:20+1d 0:30+1d ~ B ~ Walk 2m [0:20+1d 0:32+1d 12m 0tx $1440]",
      runSearch()
    );
  }

  @Test
  public void openInFirstHalfIntervalTest() {
    requestBuilder
      .profile(RaptorProfile.MULTI_CRITERIA)
      .searchParams()
      .addEgressPaths(TestAccessEgress.walk(STOP_B, D2m, T00_00, T00_25));

    assertEquals(
      join(
        "A ~ BUS R1 0:10 0:20 ~ B ~ Walk 2m [0:10 0:22 12m 0tx $1440]",
        "A ~ BUS R1 0:30 0:40 ~ B ~ Walk 2m [0:30 0:02+1d 23h32m 0tx $85440]"
      ),
      runSearch()
    );
  }

  @Test
  public void openInFirstHalfIntervalTestFullDay() {
    requestBuilder
      .profile(RaptorProfile.MULTI_CRITERIA)
      .searchParams()
      .searchWindow(D25h)
      .addEgressPaths(TestAccessEgress.walk(STOP_B, D2m, T00_00, T00_25));

    assertEquals(
      join(
        "A ~ BUS R1 0:10 0:20 ~ B ~ Walk 2m [0:10 0:22 12m 0tx $1440]",
        "A ~ BUS R1 0:30 0:40 ~ B ~ Walk 2m [0:30 0:02+1d 23h32m 0tx $85440]",
        "A ~ BUS R1 0:20+1d 0:30+1d ~ B ~ Walk 2m [0:20+1d 0:02+2d 23h42m 0tx $86040]"
      ),
      runSearch()
    );
  }

  @Test
  public void openInFirstHalfIntervalTestNextDay() {
    requestBuilder
      .profile(RaptorProfile.MULTI_CRITERIA)
      .searchParams()
      .earliestDepartureTime(SECONDS_IN_DAY + T00_10)
      .addEgressPaths(TestAccessEgress.walk(STOP_B, D2m, T00_00, T00_25));

    assertEquals(
      "A ~ BUS R1 0:20+1d 0:30+1d ~ B ~ Walk 2m [0:20+1d 0:02+2d 23h42m 0tx $86040]",
      runSearch()
    );
  }

  /*
   * The access is only open after 00:18 and before 00:20. This means that we arrive at the stop at
   * 00:20 at the earliest and 00:22 at the latest.
   */
  @Test
  public void partiallyOpenIntervalTest() {
    requestBuilder
      .profile(RaptorProfile.MULTI_CRITERIA)
      .searchParams()
      .addEgressPaths(TestAccessEgress.walk(STOP_B, D2m, T00_25, T00_35));

    assertEquals(
      join(
        "A ~ BUS R1 0:10 0:20 ~ B ~ Walk 2m [0:10 0:27 17m 0tx $1740]",
        "A ~ BUS R1 0:20 0:30 ~ B ~ Walk 2m [0:20 0:32 12m 0tx $1440]",
        "A ~ BUS R1 0:30 0:40 ~ B ~ Walk 2m [0:30 0:27+1d 23h57m 0tx $86940]"
      ),
      runSearch()
    );
  }

  @Test
  public void partiallyOpenIntervalTestFullDay() {
    requestBuilder
      .profile(RaptorProfile.MULTI_CRITERIA)
      .searchParams()
      .searchWindow(D25h)
      .addEgressPaths(TestAccessEgress.walk(STOP_B, D2m, T00_25, T00_35));

    assertEquals(
      join(
        "A ~ BUS R1 0:10 0:20 ~ B ~ Walk 2m [0:10 0:27 17m 0tx $1740]",
        "A ~ BUS R1 0:20 0:30 ~ B ~ Walk 2m [0:20 0:32 12m 0tx $1440]",
        "A ~ BUS R1 0:30 0:40 ~ B ~ Walk 2m [0:30 0:27+1d 23h57m 0tx $86940]",
        "A ~ BUS R1 0:20+1d 0:30+1d ~ B ~ Walk 2m [0:20+1d 0:32+1d 12m 0tx $1440]"
      ),
      runSearch()
    );
  }

  @Test
  public void partiallyOpenIntervalTestNextDay() {
    requestBuilder
      .profile(RaptorProfile.MULTI_CRITERIA)
      .searchParams()
      .earliestDepartureTime(SECONDS_IN_DAY + T00_10)
      .addEgressPaths(TestAccessEgress.walk(STOP_B, D2m, T00_25, T00_35));

    assertEquals(
      "A ~ BUS R1 0:20+1d 0:30+1d ~ B ~ Walk 2m [0:20+1d 0:32+1d 12m 0tx $1440]",
      runSearch()
    );
  }

  private String runSearch() {
    return pathsToString(raptorService.route(requestBuilder.build(), data));
  }

  private String runSearchDetailedResult() {
    return pathsToStringDetailed(raptorService.route(requestBuilder.build(), data));
  }
}
