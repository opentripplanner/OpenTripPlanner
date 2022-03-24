package org.opentripplanner.model.transfer;

import java.io.Serializable;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.base.ValueObjectToStringBuilder;

public final class RouteStopTransferPoint implements TransferPoint, Serializable {

  private static final long serialVersionUID = 1L;

  private final Route route;
  private final StopLocation stop;

  public RouteStopTransferPoint(Route route, StopLocation stop) {
    this.route = route;
    this.stop = stop;
  }

  public Route getRoute() {
    return route;
  }

  public StopLocation getStop() {
    return stop;
  }

  @Override
  public boolean appliesToAllTrips() {
    return true;
  }

  @Override
  public int getSpecificityRanking() { return 3; }

  @Override
  public boolean isRouteStopTransferPoint() { return true; }

  @Override
  public String toString() {
    return ValueObjectToStringBuilder.of()
            .addText("<Route ")
            .addObj(route.getId())
            .addText(", stop ")
            .addObj(stop.getId())
            .addText(">")
            .toString();
  }
}
