package org.opentripplanner.framework.time;

import java.time.Duration;
import java.util.function.LongSupplier;

/**
 * This class will track time and is useful when for example throttling something. It does NOT
 * notify anyone, but the caller must regularly check if the timer is finished.
 */
public class CountdownTimer {

  private final LongSupplier clock;
  private final long countDownDuration;
  private long timeLimit;

  /**
   * Create new timer with the given count-down-duration time.
   */
  public CountdownTimer(Duration countDownDuration) {
    this(countDownDuration, System::currentTimeMillis);
  }

  /**
   * This constructor allows us to unit test the timer without using
   * {@code System::currentTimeMillis}.
   * <p>
   * The timer is started, no need to explicit call {@link #restart()} right
   * after the construction.
   */
  CountdownTimer(Duration countDownDuration, LongSupplier clock) {
    this.clock = clock;
    this.countDownDuration = countDownDuration.toMillis();
    restart();
  }

  /**
   * Start/restart the counting down the time.
   */
  public void restart() {
    restart(now());
  }

  /**
   * The timer has past the count-down-limit (startTime + countDownDuration)
   */
  public boolean timeIsUp() {
    return timeIsUp(now());
  }

  /**
   * Return {@code true} if time is up, and then immediately start the timer again.
   */
  public boolean nextLap() {
    long time = now();
    if (timeIsUp(time)) {
      restart(time);
      return true;
    }
    return false;
  }

  private boolean timeIsUp(long time) {
    return time >= timeLimit;
  }

  private void restart(long time) {
    this.timeLimit = time + countDownDuration;
  }

  private long now() {
    return clock.getAsLong();
  }
}
