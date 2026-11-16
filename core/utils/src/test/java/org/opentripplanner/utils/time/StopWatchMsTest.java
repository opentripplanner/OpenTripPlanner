package org.opentripplanner.utils.time;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StopWatchMsTest {

  /** The time the test may take between creating the stop watch and reading it. */
  private static final long TOLERANCE_MS = 5_000;

  @Test
  void startMeasuresFromNow() {
    var subject = StopWatchMs.startStopWatch();
    assertTrue(subject.stopInMillis() >= 0);
    assertTrue(subject.stopInMillis() < TOLERANCE_MS);
  }

  @Test
  void stopMillisIsTheTimeSinceTheStartTime() {
    var subject = new StopWatchMs(System.currentTimeMillis() - 90_000);
    var elapsed = subject.stopInMillis();
    assertTrue(elapsed >= 90_000);
    assertTrue(elapsed < 90_000 + TOLERANCE_MS);
  }

  @Test
  void stopCanBeReadManyTimes() {
    var subject = new StopWatchMs(System.currentTimeMillis() - 1_000);
    var first = subject.stopInMillis();
    var second = subject.stopInMillis();
    assertTrue(second >= first);
  }

  @Test
  void stopFormatsTheElapsedTime() {
    var subject = new StopWatchMs(System.currentTimeMillis() - 65_000);
    // 1m5s, the seconds depend on the time the test takes
    assertTrue(subject.stop().startsWith("1m"), subject.stop());
  }

  @Test
  void stopFormatsAShortTimeInMilliseconds() {
    var subject = new StopWatchMs(System.currentTimeMillis() - 350);
    assertTrue(subject.stop().matches("\\d+ms"), subject.stop());
  }
}
