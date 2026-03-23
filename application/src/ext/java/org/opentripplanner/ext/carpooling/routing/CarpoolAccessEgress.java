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

  /**
   * The departure time of the passenger in seconds since transitSearchTimeZero.
   */
  private final int departureTimeOfPassenger;

  /**
   * The arrival time of the passenger in seconds since transitSearchTimeZero.
   */
  private final int arrivalTimeOfPassenger;
  private final int stop;
  private final int durationInSeconds;
  private final int c1;
  private final List<GraphPath<State, Edge, Vertex>> segments;
  private final TimeAndCost penalty;
  private final double totalWeight;
  private final double carpoolReluctance;

  public CarpoolAccessEgress(
    int stop,
    Duration duration,
    int departureTimeOfPassenger,
    int arrivalTimeOfPassenger,
    List<GraphPath<State, Edge, Vertex>> segments,
    TimeAndCost penalty,
    Double carpoolReluctance
  ) {
    this.departureTimeOfPassenger = departureTimeOfPassenger;
    this.arrivalTimeOfPassenger = arrivalTimeOfPassenger;
    this.stop = stop;
    this.durationInSeconds = (int) duration.getSeconds();
    this.carpoolReluctance = carpoolReluctance;
    this.totalWeight = this.durationInSeconds * carpoolReluctance;
    this.c1 = RaptorCostConverter.toRaptorCost(this.totalWeight);
    this.segments = segments;
    this.penalty = penalty;
  }

  @Override
  public int stop() {
    return this.stop;
  }

  @Override
  public int c1() {
    return this.c1;
  }

  @Override
  public int durationInSeconds() {
    return this.durationInSeconds;
  }

  @Override
  public int earliestDepartureTime(int requestedDepartureTime) {
    if (requestedDepartureTime > departureTimeOfPassenger) {
      return RaptorConstants.TIME_NOT_SET;
    }
    return departureTimeOfPassenger;
  }

  @Override
  public int latestArrivalTime(int requestedArrivalTime) {
    if (requestedArrivalTime < arrivalTimeOfPassenger) {
      return RaptorConstants.TIME_NOT_SET;
    }
    return arrivalTimeOfPassenger;
  }

  @Override
  public boolean hasOpeningHours() {
    return true;
  }

  public int getDepartureTimeOfPassenger() {
    return departureTimeOfPassenger;
  }

  public int getArrivalTimeOfPassenger() {
    return arrivalTimeOfPassenger;
  }

  @Override
  public RoutingAccessEgress withPenalty(TimeAndCost penalty) {
    return new CarpoolAccessEgress(
      this.stop,
      Duration.ofSeconds(this.durationInSeconds),
      this.departureTimeOfPassenger,
      this.arrivalTimeOfPassenger,
      this.segments,
      penalty,
      this.carpoolReluctance
    );
  }

  /*
    TODO: Implement this function.
    It is never used for instances of CarpoolAccessEgress, but this might change in the future.
   */
  @Override
  public List<State> getLastStates() {
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
