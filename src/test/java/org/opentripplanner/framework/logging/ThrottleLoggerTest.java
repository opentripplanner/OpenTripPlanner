package org.opentripplanner.framework.logging;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ThrottleLoggerTest {

  private static final Logger LOG = LoggerFactory.getLogger(ThrottleLoggerTest.class);
  private static final Logger THROTTLED_LOG = ThrottleLogger.throttle(LOG);

  @Test
  @Disabled("Run this test manually")
  void warn() {
    List<Integer> events = new ArrayList<>();
    for (int i = 0; i < 50_000_000; i++) {
      events.add(i);
    }
    long start = System.currentTimeMillis();

    events
      .parallelStream()
      .forEach(i ->
        THROTTLED_LOG.warn(String.format("%3.0f p", (System.currentTimeMillis() - start) / 1000.0))
      );
    /*
        EXPECTED OUTPUT

        21:28:36.618 INFO (LogThrottle.java:30) Logger org.opentripplanner.util.logging.LogThrottleTest is throttled, only one messages is logged for every 1 second interval.
        21:28:38.812 WARN (LogThrottle.java:264)   0 p
        21:28:39.812 WARN (LogThrottle.java:264)   1 p
        21:28:40.812 WARN (LogThrottle.java:264)   2 p
        21:28:41.812 WARN (LogThrottle.java:264)   3 p
        21:28:42.812 WARN (LogThrottle.java:264)   4 p
        21:28:42.812 WARN (LogThrottle.java:264)   4 p
        21:28:43.812 WARN (LogThrottle.java:264)   5 p
        21:28:44.812 WARN (LogThrottle.java:264)   6 p
        21:28:45.812 WARN (LogThrottle.java:264)   7 p
        21:28:46.812 WARN (LogThrottle.java:264)   8 p   <---  Duplicate for the 8. period
        21:28:46.812 WARN (LogThrottle.java:264)   8 p   <---  Duplicate for the 8. period
        21:28:47.812 WARN (LogThrottle.java:264)   9 p
        21:28:48.812 WARN (LogThrottle.java:264)  10 p
        */
  }
}
