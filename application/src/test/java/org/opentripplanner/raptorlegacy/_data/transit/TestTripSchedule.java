package org.opentripplanner.raptorlegacy._data.transit;

import static org.opentripplanner.transit.model.basic.Accessibility.NO_INFORMATION;

import java.time.LocalDate;
import java.util.Arrays;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.routing.algorithm.raptoradapter.api.DefaultTripPattern;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.utils.time.TimeUtils;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * An implementation of the {@link RaptorTripSchedule} for unit-testing.
 * <p>
 * The {@link DefaultTripPattern} for this schedule return {@code stopIndex == stopPosInPattern + 1 }
 *
 * @deprecated This was earlier part of Raptor and should not be used outside the Raptor
 *             module. Use the OTP model entities instead.
 */
@Deprecated
public class TestTripSchedule implements TripSchedule {

  private static final int DEFAULT_DEPARTURE_DELAY = 10;
  private static final LocalDate DATE = LocalDate.of(2025, 6, 25);
  private final DefaultTripPattern pattern;
  private final int[] arrivalTimes;
  private final int[] departureTimes;
  private final int transitReluctanceIndex;
  private final Accessibility wheelchairBoarding;
  private final TripPattern originalPattern;

  protected TestTripSchedule(
    TestTripPattern pattern,
    int[] arrivalTimes,
    int[] departureTimes,
    int transitReluctanceIndex,
    Accessibility wheelchairBoarding,
    TripPattern originalPattern
  ) {
    this.pattern = pattern;
    this.arrivalTimes = arrivalTimes;
    this.departureTimes = departureTimes;
    this.transitReluctanceIndex = transitReluctanceIndex;
    this.wheelchairBoarding = wheelchairBoarding;
    this.originalPattern = originalPattern;
  }

  public static TestTripSchedule.Builder schedule() {
    return new TestTripSchedule.Builder();
  }

  public static TestTripSchedule.Builder schedule(TestTripPattern pattern) {
    return schedule().pattern(pattern);
  }

  public static TestTripSchedule.Builder schedule(
    TestTripPattern pattern,
    TripPattern originalPattern
  ) {
    return schedule().pattern(pattern).originalPattern(originalPattern);
  }

  public static TestTripSchedule.Builder schedule(String times) {
    return new TestTripSchedule.Builder().times(times);
  }

  @Override
  public int tripSortIndex() {
    // We sort trips based on the departure from the first stop
    return arrival(0);
  }

  @Override
  public int arrival(int stopPosInPattern) {
    return arrivalTimes[stopPosInPattern];
  }

  @Override
  public int departure(int stopPosInPattern) {
    return departureTimes[stopPosInPattern];
  }

  @Override
  public DefaultTripPattern pattern() {
    return pattern;
  }

  @Override
  public int transitReluctanceFactorIndex() {
    return transitReluctanceIndex;
  }

  @Override
  public Accessibility wheelchairBoarding() {
    return wheelchairBoarding;
  }

  @Override
  public String toString() {
    if (Arrays.equals(arrivalTimes, departureTimes)) {
      return ToStringBuilder.of(TestTripSchedule.class)
        .addServiceTimeSchedule("times", arrivalTimes)
        .toString();
    }
    return ToStringBuilder.of(TestTripSchedule.class)
      .addServiceTimeSchedule("arrivals", arrivalTimes)
      .addServiceTimeSchedule("departures", departureTimes)
      .toString();
  }

  @Override
  public LocalDate getServiceDate() {
    return DATE;
  }

  @Override
  public TripTimes getOriginalTripTimes() {
    return null;
  }

  @Override
  public TripPattern getOriginalTripPattern() {
    return this.originalPattern;
  }

  @SuppressWarnings("UnusedReturnValue")
  public static class Builder {

    private TestTripPattern pattern;
    private int[] arrivalTimes;
    private int[] departureTimes;
    private int arrivalDepartureOffset = DEFAULT_DEPARTURE_DELAY;
    private int transitReluctanceIndex = 0;
    private Accessibility wheelchairBoarding = NO_INFORMATION;
    private TripPattern originalPattern;

    public TestTripSchedule.Builder pattern(TestTripPattern pattern) {
      this.pattern = pattern;
      return this;
    }

    public TestTripSchedule.Builder originalPattern(TripPattern pattern) {
      this.originalPattern = pattern;
      return this;
    }

    public TestTripSchedule.Builder pattern(String name, int... stops) {
      return pattern(TestTripPattern.pattern(name, stops));
    }

    /** @param times departure and arrival times per stop. Example: "0:10, 0:20, 0:45 .." */
    public TestTripSchedule.Builder times(String times) {
      return times(TimeUtils.times(times));
    }

    /** @param times departure and arrival times per stop in seconds past midnight. */
    public TestTripSchedule.Builder times(int... times) {
      arrivals(times);
      departures(times);
      return this;
    }

    /** @param arrivalTimes arrival times per stop. Example: "0:10, 0:20, 0:45 .. */
    public TestTripSchedule.Builder arrivals(String arrivalTimes) {
      return this.arrivals(TimeUtils.times(arrivalTimes));
    }

    /** @param arrivalTimes arrival times per stop in seconds past midnight. */
    public TestTripSchedule.Builder arrivals(int... arrivalTimes) {
      this.arrivalTimes = arrivalTimes;
      return this;
    }

    /** @param departureTimes departure times per stop. Example: "0:10, 0:20, 0:45 .. */
    public TestTripSchedule.Builder departures(String departureTimes) {
      return this.departures(TimeUtils.times(departureTimes));
    }

    /** @param departureTimes departure times per stop in seconds past midnight. */
    public TestTripSchedule.Builder departures(int... departureTimes) {
      this.departureTimes = departureTimes;
      return this;
    }

    /**
     * The time between arrival and departure for each stop in the pattern. If not both arrival and
     * departure times are set, this parameter is used to calculate the unset values.
     * <p>
     * Unit: seconds. The default is 10 seconds.
     */
    public TestTripSchedule.Builder arrDepOffset(int arrivalDepartureOffset) {
      this.arrivalDepartureOffset = arrivalDepartureOffset;
      return this;
    }

    /**
     * Set the transit-reluctance-index.
     * <p>
     * The default is 0.
     */
    public TestTripSchedule.Builder transitReluctanceIndex(int transitReluctanceIndex) {
      this.transitReluctanceIndex = transitReluctanceIndex;
      return this;
    }

    public TestTripSchedule.Builder wheelchairBoarding(Accessibility wcb) {
      this.wheelchairBoarding = wcb;
      return this;
    }

    public TestTripSchedule build() {
      if (arrivalTimes == null) {
        arrivalTimes = copyWithOffset(departureTimes, -arrivalDepartureOffset);
      } else if (departureTimes == null) {
        departureTimes = copyWithOffset(arrivalTimes, arrivalDepartureOffset);
      }
      if (arrivalTimes.length != departureTimes.length) {
        throw new IllegalStateException(
          "Number of arrival and departure times do not match." +
          " Arrivals: " +
          arrivalTimes.length +
          ", departures: " +
          arrivalTimes.length
        );
      }
      if (pattern == null) {
        pattern = TestTripPattern.pattern("DummyPattern", new int[arrivalTimes.length]);
      }
      if (arrivalTimes.length != pattern.numberOfStopsInPattern()) {
        throw new IllegalStateException(
          "Number of arrival and departure times do not match stops in pattern." +
          " Arrivals/departures: " +
          arrivalTimes.length +
          ", stops: " +
          pattern.numberOfStopsInPattern()
        );
      }

      return new TestTripSchedule(
        pattern,
        arrivalTimes,
        departureTimes,
        transitReluctanceIndex,
        wheelchairBoarding,
        originalPattern
      );
    }

    private static int[] copyWithOffset(int[] source, int offset) {
      int[] target = new int[source.length];
      for (int i = 0; i < source.length; i++) {
        target[i] = source[i] + offset;
      }
      return target;
    }
  }
}
