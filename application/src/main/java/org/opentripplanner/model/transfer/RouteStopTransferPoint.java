package org.opentripplanner.model.transfer;

import java.io.Serializable;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.utils.tostring.ValueObjectToStringBuilder;

public final class RouteStopTransferPoint implements TransferPoint, Serializable {

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
  public int getSpecificityRanking() {
    return 3;
  }

  @Override
  public boolean isRouteStopTransferPoint() {
    return true;
  }

  @Override
  public String toString() {
    return ValueObjectToStringBuilder.of()
      .addText("RouteTP{")
      .addObj(route.getId())
      .addText(", stop ")
      .addObj(stop.getId())
      .addText("}")
      .toString();
  }
}
