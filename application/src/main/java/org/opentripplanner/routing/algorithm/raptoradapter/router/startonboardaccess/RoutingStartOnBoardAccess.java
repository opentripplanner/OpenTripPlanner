package org.opentripplanner.routing.algorithm.raptoradapter.router.startonboardaccess;

import java.util.Objects;
import org.opentripplanner.framework.model.TimeAndCost;
import org.opentripplanner.raptor.api.model.RaptorStartOnBoardAccess;
import org.opentripplanner.raptor.api.model.RaptorTripScheduleStopPosition;
import org.opentripplanner.raptor.spi.RaptorTripScheduleReference;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RoutingAccessEgress;
import org.opentripplanner.street.search.state.State;

public final class RoutingStartOnBoardAccess
  implements RaptorStartOnBoardAccess, RoutingAccessEgress {

  private final int routeIndex;
  private final int tripScheduleIndex;
  private final int stopPositionInPattern;
  private final int stop;

  public RoutingStartOnBoardAccess(
    RaptorTripScheduleReference tripScheduleReference,
    LocationInTripPatternReference tripLocationInScheduleReference
  ) {
    this.routeIndex = tripScheduleReference.routeIndex();
    this.tripScheduleIndex = tripScheduleReference.tripScheduleIndex();
    this.stopPositionInPattern = tripLocationInScheduleReference.stopPositionInPattern();
    this.stop = tripLocationInScheduleReference.stopIndex();
  }

  @Override
  public int c1() {
    return 0;
  }

  @Override
  public String toString() {
    return asString(true, true, null);
  }

  @Override
  public RaptorTripScheduleStopPosition tripBoarding() {
    return new RaptorTripScheduleStopPosition(routeIndex, tripScheduleIndex, stopPositionInPattern);
  }

  @Override
  public int stop() {
    return stop;
  }

  @Override
  public boolean isWalkOnly() {
    return false;
  }

  @Override
  public TimeAndCost penalty() {
    return TimeAndCost.ZERO;
  }

  /**
   * On-board access has zero cost by definition — the rider is already on the vehicle — so
   * penalties do not apply and this returns {@code this} unchanged.
   */
  @Override
  public RoutingAccessEgress withPenalty(TimeAndCost penalty) {
    return this;
  }

  /**
   * On-board access has no street state since it does not originate from a street search.
   */
  @Override
  public State getFinalState() {
    return null;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (RoutingStartOnBoardAccess) obj;
    return (
      this.routeIndex == that.routeIndex &&
      this.tripScheduleIndex == that.tripScheduleIndex &&
      this.stopPositionInPattern == that.stopPositionInPattern &&
      this.stop == that.stop
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(routeIndex, tripScheduleIndex, stopPositionInPattern, stop);
  }
}
