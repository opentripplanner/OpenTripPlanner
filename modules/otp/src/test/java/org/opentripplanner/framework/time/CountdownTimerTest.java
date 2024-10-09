package org.opentripplanner.framework.time;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class CountdownTimerTest {

  private long time = 9_999;

  @Test
  void finished() {
    time = 500;
    var subject = new CountdownTimer(Duration.ofMillis(100), this::time);
    assertFalse(subject.timeIsUp());

    time = 599;
    assertFalse(subject.timeIsUp());

    time = 600;
    assertTrue(subject.timeIsUp());

    time = 600;
    assertTrue(subject.timeIsUp());
  }

  @Test
  void restart() {
    time = 500;
    var subject = new CountdownTimer(Duration.ofMillis(100), this::time);

    time = 620;
    subject.restart();

    time = 719;
    assertFalse(subject.timeIsUp());

    time = 720;
    assertTrue(subject.timeIsUp());
  }

  @Test
  void nextLap() {
    time = 0;
    var subject = new CountdownTimer(Duration.ofMillis(100), this::time);
    assertFalse(subject.nextLap());

    time = 99;
    assertFalse(subject.nextLap());

    time = 100;
    assertTrue(subject.nextLap());

    time = 99;
    assertFalse(subject.nextLap());

    time = 220;
    assertTrue(subject.nextLap());

    time = 319;
    assertFalse(subject.nextLap());

    time = 320;
    assertTrue(subject.nextLap());
  }

  long time() {
    return time;
  }
}
