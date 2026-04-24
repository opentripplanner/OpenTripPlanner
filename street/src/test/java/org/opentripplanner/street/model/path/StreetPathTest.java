package org.opentripplanner.street.model.path;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.TestStateBuilder;

class StreetPathTest {

  private static final Instant START_TIME = Instant.parse("2007-12-03T10:15:30.00Z");

  @Test
  void startTime() {
    var state = startState().streetEdge().build();
    var segment = new StreetPathSegment(state);
    assertEquals(START_TIME, segment.startTime());
  }

  @Test
  void endTime() {
    var state = startState()
      .testEdge(b -> b.withDurationSeconds(10))
      .testEdge(b -> b.withDurationSeconds(10))
      .build();
    var segment = new StreetPathSegment(state);

    assertEquals(START_TIME.plus(Duration.ofSeconds(20)), segment.endTime());
  }

  @Test
  void weight() {
    var state = startState()
      .testEdge(b -> b.withWeight(10))
      .testEdge(b -> b.withWeight(10))
      .build();
    var segment = new StreetPathSegment(state);

    assertEquals(20.0, segment.weight());
  }

  @Test
  void distanceMeters() {
    var state = startState()
      .testEdge(b -> b.withDistanceMeters(10))
      .testEdge(b -> b.withDistanceMeters(10))
      .build();
    var segment = new StreetPathSegment(state);
    assertEquals(20.0, segment.distanceMeters());
  }

  @Test
  void duration() {
    var state = startState()
      .testEdge(b -> b.withDurationSeconds(1))
      .testEdge(b -> b.withDurationSeconds(2))
      .testEdge(b -> b.withDurationSeconds(3))
      .testEdge(b -> b.withDurationSeconds(4))
      .build();
    var segment = new StreetPathSegment(state).subSegment(1, 4);

    assertEquals(Duration.ofSeconds(5), segment.duration());
  }

  @Test
  void geometry() {
    var state = startState()
      .testEdge()
      .testEdge(b -> b.withIncludeGeometryInPath(false))
      .build();
    var segment = new StreetPathSegment(state);

    // Make sure we ignore the last leg
    assertEquals("LINESTRING (1 1, 2 2)", segment.geometry().toString());
  }

  @Test
  void subSegment() {
    var state = startState()
      .testEdge(b -> b.withDurationSeconds(10).withWeight(10).withDistanceMeters(10))
      .testEdge(b -> b.withDurationSeconds(5).withWeight(5).withDistanceMeters(5))
      .testEdge(b -> b.withDurationSeconds(10).withWeight(10).withDistanceMeters(10))
      .build();

    var path = new StreetPathSegment(state);
    var subSegment = path.subSegment(1, 3);
    assertEquals(START_TIME.plus(Duration.ofSeconds(10)), subSegment.startTime());
    assertEquals(START_TIME.plus(Duration.ofSeconds(15)), subSegment.endTime());
    assertEquals(5.0, subSegment.weight());
    assertEquals(5.0, subSegment.distanceMeters());
  }

  private TestStateBuilder startState() {
    var request = StreetSearchRequest.of()
      .withStartTime(START_TIME)
      .withMode(StreetMode.WALK)
      .build();
    return TestStateBuilder.of(request);
  }
}
