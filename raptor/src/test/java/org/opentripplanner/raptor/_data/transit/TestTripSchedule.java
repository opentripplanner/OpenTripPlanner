package org.opentripplanner.raptor._data.transit;

import java.util.Arrays;
import java.util.stream.IntStream;
import org.opentripplanner.raptor.api.model.RaptorTripPattern;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.utils.lang.IntUtils;
import org.opentripplanner.utils.time.TimeUtils;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * An implementation of the {@link RaptorTripSchedule} for unit-testing.
 */
public class TestTripSchedule implements RaptorTripSchedule {

  private static final int DEFAULT_DEPARTURE_DELAY = 10;
  private final int[] arrivalTimes;
  private final int[] departureTimes;
  private final RaptorTripPattern pattern;

  protected TestTripSchedule(TestTripPattern pattern, int[] arrivalTimes, int[] departureTimes) {
    this.pattern = pattern;
    this.arrivalTimes = arrivalTimes;
    this.departureTimes = departureTimes;
  }

  public static TestTripSchedule.Builder schedule() {
    return new TestTripSchedule.Builder();
  }

  public static TestTripSchedule.Builder schedule(TestTripPattern pattern) {
    return schedule().pattern(pattern);
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
  public RaptorTripPattern pattern() {
    return pattern;
  }

  public int size() {
    return arrivalTimes.length;
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

  @SuppressWarnings("UnusedReturnValue")
  public static class Builder {

    private TestTripPattern pattern;
    private int[] arrivalTimes;
    private int[] departureTimes;
    private int arrivalDepartureOffset = DEFAULT_DEPARTURE_DELAY;

    public TestTripSchedule.Builder pattern(TestTripPattern pattern) {
      this.pattern = pattern;
      return this;
    }

    public TestTripSchedule.Builder copy() {
      var b = new TestTripSchedule.Builder();
      b.pattern = pattern;
      b.arrivalTimes = arrivalTimes;
      b.departureTimes = departureTimes;
      b.arrivalDepartureOffset = arrivalDepartureOffset;
      return b;
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
     * Shift all arrival/departure times by the given {@code offset}. Be careful, this
     * method change the builder instance, use {@link #copy()} if you need the original.
     * <p>
     * Offset unit is seconds.
     */
    public TestTripSchedule.Builder shiftTimes(int offset) {
      if (arrivalTimes == departureTimes) {
        arrivalTimes = departureTimes = IntUtils.shiftArray(offset, arrivalTimes);
      } else {
        if (arrivalTimes != null) {
          arrivalTimes = IntUtils.shiftArray(offset, arrivalTimes);
        }
        if (departureTimes != null) {
          departureTimes = IntUtils.shiftArray(offset, departureTimes);
        }
      }
      return this;
    }

    public TestTripSchedule.Builder[] repeat(int nTimes, int everySeconds) {
      return IntStream.range(0, nTimes)
        .mapToObj(i -> copy().shiftTimes(i * everySeconds))
        .toArray(Builder[]::new);
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

      return new TestTripSchedule(pattern, arrivalTimes, departureTimes);
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
