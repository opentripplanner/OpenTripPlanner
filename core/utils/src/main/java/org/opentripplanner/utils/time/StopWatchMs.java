package org.opentripplanner.utils.time;

/**
 * Measures the time elapsed since it was started, for logging. It can be read many times, it is
 * never stopped. It measures wall-clock time in milliseconds, so it is not suited for
 * micro-benchmarks.
 * <p>
 * This class is immutable and thread-safe.
 */
public class StopWatchMs {

  private final long startTime;

  /**
   * @param startTime the start time in epoch milliseconds, see {@link System#currentTimeMillis()}
   */
  StopWatchMs(long startTime) {
    this.startTime = startTime;
  }

  /** Create a stop watch that starts now. */
  public static StopWatchMs startStopWatch() {
    return new StopWatchMs(System.currentTimeMillis());
  }

  /** The time elapsed since the start, in milliseconds. */
  public long stopInMillis() {
    return System.currentTimeMillis() - startTime;
  }

  /**
   * The time elapsed since the start as a human-readable string, for example {@code 350ms} or
   * {@code 1m12s}. See {@link DurationUtils#durationToStrMillisescond(long)}.
   */
  public String stop() {
    return DurationUtils.durationToStrMillisescond(stopInMillis());
  }
}
