package org.opentripplanner.model.transfer;

import java.io.Serializable;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.base.ValueObjectToStringBuilder;

public final class RouteTransferPoint implements TransferPoint, Serializable {

  private static final long serialVersionUID = 1L;

  private final Route route;
  private final int stopPositionInPattern;

  public RouteTransferPoint(Route route, int stopPositionInPattern) {
    this.route = route;
    this.stopPositionInPattern = stopPositionInPattern;
  }

  public Route getRoute() {
    return route;
  }

  public int getStopPositionInPattern() {
    return stopPositionInPattern;
  }

  @Override
  public boolean applyToAllTrips() {
    return true;
  }

  @Override
  public int getSpecificityRanking() { return 2; }

  @Override
  public boolean isRouteTransferPoint() { return true; }

  @Override
  public String toString() {
    return ValueObjectToStringBuilder.of()
            .addText("<Route ")
            .addObj(route.getId())
            .addText(" @stopPos:")
            .addNum(stopPositionInPattern)
            .addText(">")
            .toString();
  }
}
