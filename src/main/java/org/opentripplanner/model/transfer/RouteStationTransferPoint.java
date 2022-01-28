package org.opentripplanner.model.transfer;

import java.io.Serializable;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.base.ValueObjectToStringBuilder;

public final class RouteStationTransferPoint implements TransferPoint, Serializable {

  private static final long serialVersionUID = 1L;

  private final Route route;
  private final Station station;

  public RouteStationTransferPoint(Route route, Station station) {
    this.route = route;
    this.station = station;
  }

  public Route getRoute() {
    return route;
  }

  public Station getStation() {
    return station;
  }

  @Override
  public boolean appliesToAllTrips() {
    return true;
  }

  @Override
  public int getSpecificityRanking() { return 2; }

  @Override
  public boolean isRouteStationTransferPoint() { return true; }

  @Override
  public String toString() {
    return ValueObjectToStringBuilder.of()
            .addText("<Route ")
            .addObj(route.getId())
            .addText(", station ")
            .addObj(station.getId())
            .addText(">")
            .toString();
  }
}
