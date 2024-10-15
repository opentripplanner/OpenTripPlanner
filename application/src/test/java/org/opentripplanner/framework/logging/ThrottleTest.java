package org.opentripplanner.framework.logging;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ThrottleTest {

  @Test
  void testSetUp() {
    Assertions.assertEquals("(throttle 1s interval)", Throttle.ofOneSecond().setupInfo());
  }

  @Test
  void smokeTest() {
    int SIZE = 2_000;
    var counter = new AtomicInteger(0);
    var events = createIntegerSequence(SIZE);
    var subject = Throttle.ofOneSecond();

    events.parallelStream().forEach(i -> subject.throttle(counter::incrementAndGet));
    Assertions.assertTrue(
      counter.get() > 0,
      "The counter should be greater than 0: " + counter.get()
    );
    Assertions.assertTrue(
      counter.get() < 100,
      "The counter should be less than 10: " + counter.get()
    );
  }

  @Test
  @Disabled("Run this test manually")
  void manualTest() {
    var quietPeriod = Duration.ofMillis(50);
    var subject = new Throttle(quietPeriod);

    List<Integer> events = createIntegerSequence(20_000_000);
    long start = System.currentTimeMillis();

    events
      .parallelStream()
      .forEach(i ->
        subject.throttle(() ->
          System.err.printf(Locale.ROOT, "%d ms%n", (System.currentTimeMillis() - start))
        )
      );
    /*
      We get a lot of duplicates here because of "optimistic read/write" on shared memory - this is ok, as long as
      it does not fail.

      EXPECTED OUTPUT
         4 ms
        54 ms
        54 ms  // Duplicate
        :
        104 ms
        104 ms  // Duplicate
        :
        155 ms
        206 ms
        256 ms  // Duplicate
        306 ms
        306 ms  // Duplicate
        :
        */
  }

  private List<Integer> createIntegerSequence(int size) {
    List<Integer> events = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      events.add(i);
    }
    return events;
  }
}
