package org.opentripplanner.raptor.rangeraptor.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.request.DynamicSearchWindowCoefficients;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.api.request.SearchParams;

public class RaptorSearchWindowCalculatorTest {

  private static final DynamicSearchWindowCoefficients C = new DynamicSearchWindowCoefficients() {
    @Override
    public double minTransitTimeCoefficient() {
      return 0.6;
    }

    @Override
    public double minWaitTimeCoefficient() {
      return 0.4;
    }

    /** 10 minutes = 600s */
    @Override
    public Duration minWindow() {
      return Duration.ofMinutes(10);
    }

    /** Set the max search-window length to 30 minutes (1_800 seconds) */
    @Override
    public Duration maxWindow() {
      return Duration.ofMinutes(30);
    }

    /** Round search-window to nearest 1 minute (60 seconds) */
    @Override
    public int stepMinutes() {
      return 1;
    }
  };

  private final RaptorSearchWindowCalculator subject = new RaptorSearchWindowCalculator(C);

  @Test
  public void calcEarliestDeparture() {
    SearchParams searchParams = new RaptorRequestBuilder<TestTripSchedule>()
      .searchParams()
      .latestArrivalTime(3_000)
      .buildSearchParam();

    int minTransitTime = 500;
    int minWaitTime = 200;

    subject.withHeuristics(minTransitTime, minWaitTime).withSearchParams(searchParams).calculate();

    /*
           search-window: round_60(C + T * minTransitTime + W * minWaitTime)
               = round_60(600 + 0.6 * 500 + 0.4 * 200)
               = round_60(980) = 960

           EDT = LAT - (search-window + minTripTime)
           EDT = 3000 - (960s + round_60(500))
           EDT = 3000 - (960s + 480s)
           EDT = 1560
         */
    assertEquals(960, subject.getSearchWindowSeconds());
    assertEquals(1_560, subject.getEarliestDepartureTime());
    // Given - verify not changed
    assertEquals(3_000, subject.getLatestArrivalTime());
    assertTrue(
      minTransitTime >
      subject.getLatestArrivalTime() -
      (subject.getSearchWindowSeconds() + subject.getEarliestDepartureTime())
    );
  }

  @Test
  public void calcEarliestDepartureExact() {
    SearchParams searchParams = new RaptorRequestBuilder<TestTripSchedule>()
      .searchParams()
      .latestArrivalTime(3_000)
      .buildSearchParam();

    int minTransitTime = 600;
    int minWaitTime = 0;

    subject.withHeuristics(minTransitTime, minWaitTime).withSearchParams(searchParams).calculate();

    assertEquals(960, subject.getSearchWindowSeconds());
    assertEquals(1_440, subject.getEarliestDepartureTime());
    // Given - verify not changed
    assertEquals(3_000, subject.getLatestArrivalTime());

    assertEquals(
      minTransitTime,
      subject.getLatestArrivalTime() -
      (subject.getSearchWindowSeconds() + subject.getEarliestDepartureTime())
    );
  }

  @Test
  public void calcLatestArrivalTime() {
    SearchParams searchParams = new RaptorRequestBuilder<TestTripSchedule>()
      .searchParams()
      .earliestDepartureTime(10_200)
      .buildSearchParam();

    int minTransitTime = 300;
    int minWaitTime = 100;

    subject.withHeuristics(minTransitTime, minWaitTime).withSearchParams(searchParams).calculate();

    /*
           search-window: round_N(C + T * minTransitTime + W * minWaitTime)
               = round_60(600 + 0.6 * 300 + 0.4 * 100)
               = round_60(820) = 840

           EDT = 10_200
           LAT = 10_200 + (840 + roundUp_60(300)) = 11_340
         */
    assertEquals(840, subject.getSearchWindowSeconds());
    // Given - verify not changed
    assertEquals(10_200, subject.getEarliestDepartureTime());
    assertEquals(RaptorConstants.TIME_NOT_SET, subject.getLatestArrivalTime());
  }

  @Test
  public void calcSearchWindowLimitByMaxLength() {
    SearchParams searchParams = new RaptorRequestBuilder<TestTripSchedule>()
      .searchParams()
      .earliestDepartureTime(12_000)
      .buildSearchParam();

    int minTransitTime = 1_500;
    int minWaitTime = 1_200;

    subject.withHeuristics(minTransitTime, minWaitTime).withSearchParams(searchParams).calculate();

    /*
           search-window: round_N(C + T * minTransitTime + W * minWaitTime)
               = round_60(600 + 0.6 * 1_500 + 0.4 * 1_200) = 1_980

           EDT = 12_000
           search-window = min(1_980, 1_800) = 1_800
           LAT = 12_000 + (1_500 + roundUp_60(1_800)) = 15_300
         */
    assertEquals(1_800, subject.getSearchWindowSeconds());
    // Given - verify not changed
    assertEquals(12_000, subject.getEarliestDepartureTime());
    assertEquals(RaptorConstants.TIME_NOT_SET, subject.getLatestArrivalTime());
  }

  @Test
  public void calcSearchWindowMin() {
    SearchParams searchParams = new RaptorRequestBuilder<TestTripSchedule>()
      .searchParams()
      .earliestDepartureTime(12_000)
      .buildSearchParam();

    int minTransitTime = 49;
    int minWaitTime = 0;

    subject.withHeuristics(minTransitTime, minWaitTime).withSearchParams(searchParams).calculate();

    assertEquals(600, subject.getSearchWindowSeconds());
  }

  @Test
  public void calcSearchWindowFromLATAndEDT() {
    SearchParams searchParams = new RaptorRequestBuilder<TestTripSchedule>()
      .searchParams()
      .earliestDepartureTime(12_000)
      .latestArrivalTime(18_000)
      .buildSearchParam();

    int minTransitTime = 3_000;
    int minWaitTime = 6_000;

    subject.withHeuristics(minTransitTime, minWaitTime).withSearchParams(searchParams).calculate();

    /*
           search-window: round_60(LAT - EDT - minTransitTime)
               = round_60(18 000 - 12 000 - 3 000)
               = 3 000
         */
    assertEquals(3_000, subject.getSearchWindowSeconds());
  }

  @Test
  public void calculateNotDefinedIfMinTravelTimeNotSet() {
    assertThrows(IllegalArgumentException.class, subject::calculate);
  }

  static List<V2> roundUpToNearestMinuteTestCase() {
    return List.of(V2.of(0, 0), V2.of(0, 59), V2.of(60, 60), V2.of(60, 119), V2.of(120, 120));
  }

  @ParameterizedTest
  @MethodSource("roundUpToNearestMinuteTestCase")
  void roundUpToNearestMinute(V2 v2) {
    assertEquals(v2.expected(), subject.roundDownToNearestMinute(v2.value()));
  }

  @Test
  void roundUpToNearestMinuteNotDefinedForNegativeNumbers() {
    var ex = assertThrows(IllegalArgumentException.class, () -> subject.roundDownToNearestMinute(-1)
    );
    assertEquals("This operation is not defined for negative numbers: -1", ex.getMessage());
  }

  @Test
  public void roundStep() {
    assertEquals(-60, subject.roundStep(-31f));
    assertEquals(0, subject.roundStep(-30f));
    assertEquals(0, subject.roundStep(0f));
    assertEquals(0, subject.roundStep(29f));
    assertEquals(60, subject.roundStep(30f));
    assertEquals(480, subject.roundStep(450f));
  }

  record V2(int expected, int value) {
    static V2 of(int expected, int value) {
      return new V2(expected, value);
    }
  }
}
