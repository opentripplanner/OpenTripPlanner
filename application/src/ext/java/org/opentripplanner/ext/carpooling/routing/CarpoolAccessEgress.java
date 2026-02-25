package org.opentripplanner.ext.carpooling.routing;

import java.time.Duration;
import java.util.List;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.framework.model.TimeAndCost;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorCostConverter;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RoutingAccessEgress;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

public class CarpoolAccessEgress implements RoutingAccessEgress {

  private final int startOfTrip;
  private final int endOfTrip;
  private final int stop;
  private final int durationInSeconds;
  private final int cost;
  private final Duration extraTimeForStop;
  private final List<GraphPath<State, Edge, Vertex>> segments;
  private final TimeAndCost penalty;
  private final double totalWeight;
  private final double carReluctance;

  /*
     Setting the cost and weight to the duration of the trip times carReluctance. Not sure
     if this is the way it should be, but it works for now. This is done differently for
     carpooling in direct mode, and the cost should be set ( presumably ?) the
     same way for both access/egress and direct.
   */
  public CarpoolAccessEgress(
    int stop,
    Duration duration,
    Duration extraTimeForStop,
    int startOfTrip,
    int endOfTrip,
    List<GraphPath<State, Edge, Vertex>> segments,
    TimeAndCost penalty,
    Double carReluctance
  ) {
    this.startOfTrip = startOfTrip;
    this.endOfTrip = endOfTrip;
    this.stop = stop;
    this.durationInSeconds = (int) duration.getSeconds();
    this.carReluctance = carReluctance;
    this.totalWeight = this.durationInSeconds * carReluctance;
    this.cost = RaptorCostConverter.toRaptorCost(this.totalWeight);
    this.segments = segments;
    this.penalty = penalty;
    this.extraTimeForStop = extraTimeForStop;
  }

  @Override
  public int stop() {
    return this.stop;
  }

  @Override
  public int c1() {
    return this.cost;
  }

  @Override
  public int durationInSeconds() {
    return this.durationInSeconds;
  }

  @Override
  public int earliestDepartureTime(int requestedDepartureTime) {
    if (requestedDepartureTime > startOfTrip) {
      return RaptorConstants.TIME_NOT_SET;
    }
    return startOfTrip;
  }

  @Override
  public int latestArrivalTime(int requestedArrivalTime) {
    if (requestedArrivalTime < endOfTrip) {
      return RaptorConstants.TIME_NOT_SET;
    }
    return endOfTrip;
  }

  @Override
  public boolean hasOpeningHours() {
    return true;
  }

  public int getStartOfTrip() {
    return startOfTrip;
  }

  public int getEndOfTrip() {
    return endOfTrip;
  }

  @Override
  public RoutingAccessEgress withPenalty(TimeAndCost penalty) {
    return new CarpoolAccessEgress(
      this.stop,
      Duration.ofSeconds(this.durationInSeconds),
      this.extraTimeForStop,
      this.startOfTrip,
      this.endOfTrip,
      this.segments,
      penalty,
      this.carReluctance
    );
  }

  /*
    Not sure how to support this. We would need to somehow merge the states of the segments.
   */
  @Override
  public State getLastState() {
    throw new UnsupportedOperationException(
      "Fetching last state of CarpoolAccessEgress is not yet implemented"
    );
  }

  @Override
  public boolean isWalkOnly() {
    return false;
  }

  @Override
  public TimeAndCost penalty() {
    return this.penalty;
  }

  public List<GraphPath<State, Edge, Vertex>> getSegments() {
    return this.segments;
  }

  public double getTotalWeight() {
    return this.totalWeight;
  }
}
