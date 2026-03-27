package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import java.util.Objects;
import org.opentripplanner.framework.model.TimeAndCost;
import org.opentripplanner.raptor.api.model.RaptorOnBoardAccess;
import org.opentripplanner.street.search.state.State;

public final class RoutingOnBoardAccess implements RaptorOnBoardAccess, RoutingAccessEgress {

  private final int routeIndex;
  private final int tripScheduleIndex;
  private final int stopPositionInPattern;
  private final int stop;
  private final int boardingTime;

  public RoutingOnBoardAccess(
    int routeIndex,
    int tripScheduleIndex,
    int stopPositionInPattern,
    int stop,
    int boardingTime
  ) {
    this.routeIndex = routeIndex;
    this.tripScheduleIndex = tripScheduleIndex;
    this.stopPositionInPattern = stopPositionInPattern;
    this.stop = stop;
    this.boardingTime = boardingTime;
  }

  public int boardingTime() {
    return boardingTime;
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
  public int routeIndex() {
    return routeIndex;
  }

  @Override
  public int tripScheduleIndex() {
    return tripScheduleIndex;
  }

  @Override
  public int stopPositionInPattern() {
    return stopPositionInPattern;
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
  public State getLastState() {
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
    var that = (RoutingOnBoardAccess) obj;
    return (
      this.routeIndex == that.routeIndex &&
      this.tripScheduleIndex == that.tripScheduleIndex &&
      this.stopPositionInPattern == that.stopPositionInPattern &&
      this.stop == that.stop &&
      this.boardingTime == that.boardingTime
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(routeIndex, tripScheduleIndex, stopPositionInPattern, stop, boardingTime);
  }
}
