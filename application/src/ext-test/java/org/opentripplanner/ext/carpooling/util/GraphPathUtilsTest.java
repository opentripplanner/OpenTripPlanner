package org.opentripplanner.ext.carpooling.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class GraphPathUtilsTest {

  private static final Duration TEN_MINUTES = Duration.ofMinutes(10);
  private static final Duration ONE_MINUTE = Duration.ofMinutes(1);

  @Test
  void calculateCumulativeDurations_sixSegmentsWithStopDelay() {
    Duration[] segments = {
      TEN_MINUTES,
      TEN_MINUTES,
      TEN_MINUTES,
      TEN_MINUTES,
      TEN_MINUTES,
      TEN_MINUTES,
    };

    Duration[] result = GraphPathUtils.calculateCumulativeDurations(segments, ONE_MINUTE);

    assertArrayEquals(
      new Duration[] {
        Duration.ofMinutes(0),
        Duration.ofMinutes(10),
        Duration.ofMinutes(21),
        Duration.ofMinutes(32),
        Duration.ofMinutes(43),
        Duration.ofMinutes(54),
        Duration.ofMinutes(65),
      },
      result
    );
  }

  @Test
  void calculateCumulativeDurations_noStopDelay() {
    Duration[] segments = { TEN_MINUTES, TEN_MINUTES, TEN_MINUTES };

    Duration[] result = GraphPathUtils.calculateCumulativeDurations(segments, Duration.ZERO);

    assertArrayEquals(
      new Duration[] {
        Duration.ofMinutes(0),
        Duration.ofMinutes(10),
        Duration.ofMinutes(20),
        Duration.ofMinutes(30),
      },
      result
    );
  }

  @Test
  void calculateCumulativeDurations_singleSegment_noStopDelayApplied() {
    Duration[] segments = { TEN_MINUTES };

    Duration[] result = GraphPathUtils.calculateCumulativeDurations(segments, ONE_MINUTE);

    assertArrayEquals(new Duration[] { Duration.ZERO, TEN_MINUTES }, result);
  }

  @Test
  void calculateCumulativeDurations_twoSegments_stopDelayOnlyAtSecondPoint() {
    Duration[] segments = { TEN_MINUTES, TEN_MINUTES };

    Duration[] result = GraphPathUtils.calculateCumulativeDurations(segments, ONE_MINUTE);

    assertArrayEquals(
      new Duration[] { Duration.ofMinutes(0), Duration.ofMinutes(10), Duration.ofMinutes(21) },
      result
    );
  }

  @Test
  void calculateCumulativeDurations_noSegments() {
    Duration[] segments = {};

    Duration[] result = GraphPathUtils.calculateCumulativeDurations(segments, ONE_MINUTE);

    assertArrayEquals(new Duration[] { Duration.ZERO }, result);
  }

  @Test
  void calculateCumulativeDurations_varyingSegmentDurations() {
    Duration[] segments = { Duration.ofMinutes(5), Duration.ofMinutes(15), Duration.ofMinutes(10) };

    Duration[] result = GraphPathUtils.calculateCumulativeDurations(
      segments,
      Duration.ofMinutes(2)
    );

    assertEquals(Duration.ofMinutes(0), result[0]);
    assertEquals(Duration.ofMinutes(5), result[1]);
    assertEquals(Duration.ofMinutes(22), result[2]);
    assertEquals(Duration.ofMinutes(34), result[3]);
  }
}
