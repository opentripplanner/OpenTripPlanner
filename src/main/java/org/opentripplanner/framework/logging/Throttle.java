package org.opentripplanner.framework.logging;

import java.time.Duration;
import org.opentripplanner.framework.time.TimeUtils;

/**
 * This class can be used to throttle (logging) events.
 * <p>
 * The primary use-case for this class is to prevent a logger for degrading the performance,
 * because too many events are logged during a short period of time. This could happen if you are
 * parsing thousands or millions of records and each of them will cause a log event to happen.
 * <p>
 * To use it, wrap the log statement:
 * <pre>
 * THROTTLE.throttle(() -> LOG.warn("Cost mismatch ...", ...));
 * </pre>
 * By wrapping the log statement only one log event will occur per second.
 * <p>
 * THREAD SAFETY - The implementation is very simple and do not do any synchronization, so it is
 * possible that more than 1 log event is logged for each second, but that is the only thread
 * safety issue. It is safe to use in a multithreaded cases. See the JavaDoc on the private
 * {@code throttle()} method for implementation details.
 */
public class Throttle {

  private final int quietPeriodMilliseconds;
  private long timeout = Long.MIN_VALUE;
  private final String setupInfo;

  /**
   * Package local to be able to unit test.
   */
  Throttle(Duration quietPeriod) {
    this.quietPeriodMilliseconds = (int) quietPeriod.toMillis();
    this.setupInfo = "(throttle " + TimeUtils.msToString(quietPeriodMilliseconds) + " interval)";
  }

  public static Throttle ofOneSecond() {
    return new Throttle(Duration.ofSeconds(1));
  }

  public static Throttle ofOneMinute() {
    return new Throttle(Duration.ofMinutes(1));
  }

  public String setupInfo() {
    return setupInfo;
  }

  public void throttle(Runnable body) {
    if (!throttle()) {
      body.run();
    }
  }

  /**
   * This method check if the throttle timeout is set and return {@code true} if it is. It also set
   * the next timeout. The write/read operations are NOT synchronized which may cause two or more
   * concurrent calls to both return {@code false}, hence causing two log events for the same
   * throttle time period - which is a minor drawback. The throttle do however guarantee that at
   * least one event is logged for each throttle time period. This is guaranteed based on the
   * assumption that writing to the {@code timeout} (primitive long) is an atomic operation.
   * <p>
   * In the worst case scenario, each thread keep their local version of the {@code timeout} and one
   * log message from each thread is printed every second. This can behave differently from one JVM
   * to another.
   */
  public boolean throttle() {
    long time = System.currentTimeMillis();

    if (time < timeout) {
      return true;
    }
    timeout = time + quietPeriodMilliseconds;
    return false;
  }
}
