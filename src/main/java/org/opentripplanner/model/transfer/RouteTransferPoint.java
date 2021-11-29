package org.opentripplanner.model.transfer;

import java.io.Serializable;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Trip;

/**
 * This is a specialized version of the {@link TripTransferPoint}, it represent a
 * given trip of the GTFS Route transfer. It override the specificity-ranking. Except for that,
 * it behave like its super type. So, when looking up tran
 * <p>
 * By expanding a route into trips, we can drop expanded-trips(lower specificity ranking)
 * if a "real" trip-transfers-point exist.
 */
public class RouteTransferPoint extends TripTransferPoint implements Serializable {

  private static final long serialVersionUID = 1L;

  private final Route route;

  public RouteTransferPoint(Route route, Trip trip, int stopPosition) {
    super(trip, stopPosition);
    this.route = route;
  }

  @Override
  public int getSpecificityRanking() { return 1; }

  @Override
  public String toString() {
    return "(route: " + route.getId()
            + ", trip: " + getTrip().getId()
            + ", stopPos: " + getStopPosition() + ")";
  }
}
