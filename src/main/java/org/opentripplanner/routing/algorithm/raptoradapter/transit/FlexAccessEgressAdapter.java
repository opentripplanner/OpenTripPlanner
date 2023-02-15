package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import javax.annotation.Nullable;
import org.opentripplanner.ext.flex.FlexAccessEgress;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.raptor.api.RaptorConstants;

/**
 * This class is used to adapt the FlexAccessEgress into a time-dependent multi-leg DefaultAccessEgress.
 */
public class FlexAccessEgressAdapter extends DefaultAccessEgress {

  private final FlexAccessEgress flexAccessEgress;

  public FlexAccessEgressAdapter(FlexAccessEgress flexAccessEgress, boolean isEgress) {
    super(
      flexAccessEgress.stop.getIndex(),
      isEgress ? flexAccessEgress.lastState.reverse() : flexAccessEgress.lastState
    );
    this.flexAccessEgress = flexAccessEgress;
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
    return flexAccessEgress.directToStop;
  }

  @Override
  public boolean hasOpeningHours() {
    // TODO OTP2: THIS SHOULD BE IMPLEMENTED SO WE CAN FILTER FLEX ACCESS AND EGRESS
    //            IN ROUTING, IT IS SET TO TRUE NOW TO ASSUME ALL FLEX HAS OPENING HOURS
    return true;
  }

  @Nullable
  @Override
  public String openingHoursToString() {
    //  TODO - Return "[earliest-board/alight-time latest-board-time]" or
    //       - "[exact-board/alight-time]" for the given access/egress stop.
    //       - For egress used board-time for access used alight-time.
    return "TODO";
  }

  @Override
  public String toString() {
    return asString(true);
  }

  private static int mapToRaptorTime(int flexTime) {
    return flexTime == StopTime.MISSING_VALUE ? RaptorConstants.TIME_NOT_SET : flexTime;
  }
}
