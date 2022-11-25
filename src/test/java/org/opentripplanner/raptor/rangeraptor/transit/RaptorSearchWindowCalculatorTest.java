package org.opentripplanner.raptor.rangeraptor.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
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
    public int minWinTimeMinutes() {
      return 10;
    }

    /** Set the max search-window length to 30 minutes (1_800 seconds) */
    @Override
    public int maxWinTimeMinutes() {
      return 30;
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
           search-window: round_N(C + T * minTransitTime + W * minWaitTime)
               = round_60(600 + 0.6 * 500 + 0.4 * 200)
               = round_60(980) = 960

           EDT = LAT - (search-window + minTripTime)
           EDT = 3000 - (960s + roundUp_60(500))
           EDT = 3000 - (960s + 540s)
           EDT = 1500
         */
    assertEquals(960, subject.getSearchWindowSeconds());
    assertEquals(1_500, subject.getEarliestDepartureTime());
    // Given - verify not changed
    assertEquals(3_000, subject.getLatestArrivalTime());
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
    assertEquals(SearchParams.TIME_NOT_SET, subject.getLatestArrivalTime());
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
    assertEquals(SearchParams.TIME_NOT_SET, subject.getLatestArrivalTime());
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

  @Test
  public void roundUpToNearestMinute() {
    assertEquals(0, subject.roundUpToNearestMinute(0));
    assertEquals(60, subject.roundUpToNearestMinute(1));
    assertEquals(60, subject.roundUpToNearestMinute(60));
    assertEquals(120, subject.roundUpToNearestMinute(61));
  }

  @Test
  public void roundUpToNearestMinuteNotDefinedForNegativeNumbers() {
    assertThrows(IllegalArgumentException.class, () -> subject.roundUpToNearestMinute(-1));
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
}
