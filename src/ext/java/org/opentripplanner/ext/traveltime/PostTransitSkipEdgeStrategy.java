package org.opentripplanner.ext.traveltime;

import java.time.Duration;
import java.time.Instant;
import org.opentripplanner.astar.spi.SkipEdgeStrategy;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.search.state.State;

public class PostTransitSkipEdgeStrategy implements SkipEdgeStrategy<State, Edge> {

  private final long maxDurationSeconds;
  private final long departureTime;
  private final boolean arriveBy;

  public PostTransitSkipEdgeStrategy(
    Duration maxEgressTime,
    Instant departureTime,
    boolean arriveBy
  ) {
    this.maxDurationSeconds = maxEgressTime.toSeconds();
    this.departureTime = departureTime.getEpochSecond();
    this.arriveBy = arriveBy;
  }

  @Override
  public boolean shouldSkipEdge(State current, Edge edge) {
    long postTransitDepartureTime;
    if (current.stateData instanceof TravelTimeStateData travelTimeStateData) {
      postTransitDepartureTime = travelTimeStateData.postTransitDepartureTime;
    } else {
      postTransitDepartureTime = departureTime;
    }
    long duration;

    if (arriveBy) {
      duration = postTransitDepartureTime - current.getTimeSeconds();
    } else {
      duration = current.getTimeSeconds() - postTransitDepartureTime;
    }

    return duration > maxDurationSeconds;
  }
}
