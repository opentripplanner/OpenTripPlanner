package org.opentripplanner.model.transfer;

import java.io.Serializable;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.Station;

public final class RouteStationTransferPoint implements TransferPoint, Serializable {

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
  public int getSpecificityRanking() {
    return 2;
  }

  @Override
  public boolean isRouteStationTransferPoint() {
    return true;
  }

  @Override
  public String toString() {
    return "RouteTP{" + route.getId() + ", station " + station.getId() + '}';
  }
}
