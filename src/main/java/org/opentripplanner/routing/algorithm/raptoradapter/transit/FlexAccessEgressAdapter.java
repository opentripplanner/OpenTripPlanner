package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import org.opentripplanner.ext.flex.FlexAccessEgress;
import org.opentripplanner.framework.model.TimeAndCost;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.raptor.api.model.RaptorConstants;

/**
 * This class is used to adapt the FlexAccessEgress into a time-dependent multi-leg DefaultAccessEgress.
 */
public class FlexAccessEgressAdapter extends DefaultAccessEgress {

  private final FlexAccessEgress flexAccessEgress;

  public FlexAccessEgressAdapter(FlexAccessEgress flexAccessEgress, boolean isEgress) {
    super(
      flexAccessEgress.stop().getIndex(),
      isEgress ? flexAccessEgress.lastState().reverse() : flexAccessEgress.lastState()
    );
    this.flexAccessEgress = flexAccessEgress;
  }

  private FlexAccessEgressAdapter(FlexAccessEgressAdapter other, TimeAndCost penalty) {
    super(other, penalty);
    this.flexAccessEgress = other.flexAccessEgress;
  }

  @Override
  public int earliestDepartureTime(int requestedDepartureTime) {
    return mapToRaptorTime(flexAccessEgress.earliestDepartureTime(requestedDepartureTime));
  }

  @Override
  public int latestArrivalTime(int requestedArrivalTime) {
    return mapToRaptorTime(flexAccessEgress.latestArrivalTime(requestedArrivalTime));
  }

  @Override
  public int numberOfRides() {
    // We only support one flex leg at the moment
    return 1;
  }

  @Override
  public boolean stopReachedOnBoard() {
    return flexAccessEgress.stopReachedOnBoard();
  }

  @Override
  public boolean hasOpeningHours() {
    // TODO OTP2: THIS SHOULD BE IMPLEMENTED SO WE CAN FILTER FLEX ACCESS AND EGRESS
    //            IN ROUTING, IT IS SET TO TRUE NOW TO ASSUME ALL FLEX HAS OPENING HOURS
    return true;
  }

  @Override
  public boolean isWalkOnly() {
    return false;
  }

  @Override
  public DefaultAccessEgress withPenalty(TimeAndCost penalty) {
    return new FlexAccessEgressAdapter(this, penalty);
  }

  private static int mapToRaptorTime(int flexTime) {
    return flexTime == StopTime.MISSING_VALUE ? RaptorConstants.TIME_NOT_SET : flexTime;
  }
}
