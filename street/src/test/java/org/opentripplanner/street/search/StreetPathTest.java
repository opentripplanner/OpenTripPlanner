package org.opentripplanner.street.search;

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
    var path = new StreetPath(state);
    assertEquals(START_TIME, path.startTime());
  }

  @Test
  void endTime() {
    var state = startState()
      .testEdge(b -> b.withSeconds(10))
      .testEdge(b -> b.withSeconds(10))
      .build();
    var path = new StreetPath(state);

    assertEquals(START_TIME.plus(Duration.ofSeconds(20)), path.endTime());
  }

  @Test
  void weight() {
    var state = startState()
      .testEdge(b -> b.withWeight(10))
      .testEdge(b -> b.withWeight(10))
      .build();
    var path = new StreetPath(state);

    assertEquals(20.0, path.weight());
  }

  @Test
  void distanceMeters() {
    var state = startState()
      .testEdge(b -> b.withDistance(10))
      .testEdge(b -> b.withDistance(10))
      .build();
    var path = new StreetPath(state);
    assertEquals(20.0, path.distanceMeters());
  }

  @Test
  void duration() {
    var state = startState()
      .testEdge(b -> b.withSeconds(1))
      .testEdge(b -> b.withSeconds(2))
      .testEdge(b -> b.withSeconds(3))
      .testEdge(b -> b.withSeconds(4))
      .build();
    var path = new StreetPath(state).subPath(1, 4);

    assertEquals(Duration.ofSeconds(5), path.duration());
  }

  @Test
  void geometry() {
    var state = startState()
      .testEdge()
      .testEdge(b -> b.withIncludeGeometryInPath(false))
      .build();
    var path = new StreetPath(state);

    // Make sure we ignore the last leg
    assertEquals("LINESTRING (1 1, 2 2)", path.geometry().toString());
  }

  @Test
  void subPath() {
    var state = startState()
      .testEdge(b -> b.withSeconds(10).withWeight(10).withDistance(10))
      .testEdge(b -> b.withSeconds(5).withWeight(5).withDistance(5))
      .testEdge(b -> b.withSeconds(10).withWeight(10).withDistance(10))
      .build();

    var path = new StreetPath(state);
    var subPath = path.subPath(1,3);
    assertEquals(START_TIME.plus(Duration.ofSeconds(10)), subPath.startTime());
    assertEquals(START_TIME.plus(Duration.ofSeconds(15)), subPath.endTime());
    assertEquals(5.0, subPath.weight());
    assertEquals(5.0, subPath.distanceMeters());
  }

  private TestStateBuilder startState() {
    var request = StreetSearchRequest.of()
      .withStartTime(START_TIME)
      .withMode(StreetMode.WALK)
      .build();
   return TestStateBuilder.of(request);
  }
}