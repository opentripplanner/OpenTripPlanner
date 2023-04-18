package org.opentripplanner.street.search.state;

import java.time.Instant;
import java.time.OffsetDateTime;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;

/**
 * Builds up a state chain for use in tests.
 */
public class TestStateBuilder {

  private static final Instant DEFAULT_START_TIME = OffsetDateTime
    .parse("2023-04-18T12:00:00+02:00")
    .toInstant();
  private int count = 1;

  private State currentState;

  private TestStateBuilder(StreetMode mode) {
    currentState =
      new State(
        StreetModelForTest.intersectionVertex(count, count),
        StreetSearchRequest.of().withMode(mode).withStartTime(DEFAULT_START_TIME).build()
      );
  }

  public static TestStateBuilder ofWalking() {
    return new TestStateBuilder(StreetMode.WALK);
  }

  public static TestStateBuilder ofDriving() {
    return new TestStateBuilder(StreetMode.CAR);
  }

  public TestStateBuilder streetEdge() {
    count++;
    var from = (StreetVertex) currentState.vertex;
    var to = StreetModelForTest.intersectionVertex(count, count);

    var edge = StreetModelForTest.streetEdge(from, to);

    currentState = edge.traverse(currentState);
    return this;
  }

  public State build() {
    return currentState;
  }
}
