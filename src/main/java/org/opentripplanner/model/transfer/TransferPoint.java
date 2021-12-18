package org.opentripplanner.model.transfer;

import javax.annotation.Nullable;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Trip;

/**
 * This interface is used to represent a point or location where a transfer start from or end.
 *
 * <p>There are 4 different Transfer points:
 * <ol>
 *   <li>
 *     {@link StopTransferPoint} This apply to all trip stopping at the given stop.
 *
 *     <p>This is the least specific type, and is overridden if a more specific type exist.
 *   </li>
 *   <li>
 *     {@link StationTransferPoint} This apply to all trip stopping at a stop part of the given
 *     station.
 *     <p>The specificity-ranking is above {@link StationTransferPoint}s and less than
 *     {@link RouteTransferPoint}.
 *   </li>
 *   <li>
 *     A {@link RouteTransferPoint} is a from/to point for a Route at the given stop/station. This
 *     only exist in GTFS, not in the Nordic NeTex profile.
 *
 *     <p>The specificity-ranking is above {@link StopTransferPoint}s and less than
 *     {@link TripTransferPoint}.
 *   </li>
 *   <li>
 *     {@link TripTransferPoint} A transfer from/to a Trip at the given stop position(not stop).
 *
 *     <p>This is the most specific point type, and will override both {@link RouteTransferPoint}
 *     and {@link StopTransferPoint} if more than one match exist.
 *   </li>
 * </ol>
 * <p>
 * The GTFS Transfers may specify a transfer from/to a route/trip and stop/station. But in OTP we
 * map the stop to a stop position in pattern. The OTP model {@link RouteTransferPoint} and
 * {@link TripTransferPoint} do NOT reference the stop/station, but the
 * {@code stopPositionInPattern} instead. The reason is that real-time updates could invalidate a
 * (route+stop) transfer-point, since the stop could change to another  platform(common for railway
 * stations). To account for this the RT-update would have to patch the (route&stop)-transfer-point.
 * We simplify the RT-updates by converting expanding (route+stop) to (trip+stop position).
 */
public interface TransferPoint {

  /** Return {@code true} if this transfer point apply to all trips in pattern */
  boolean applyToAllTrips();

  /**
   * <a href="https://developers.google.com/transit/gtfs/reference/gtfs-extensions#specificity-of-a-transfer">
   * Specificity of a transfer
   * </a>
   */
  int getSpecificityRanking();

  /** is a Trip specific transfer point */
  default boolean isTripTransferPoint() { return false; }

  default TripTransferPoint asTripTransferPoint() { return (TripTransferPoint) this; }

  /** is a Route specific transfer point */
  default boolean isRouteTransferPoint() { return false; }

  default RouteTransferPoint asRouteTransferPoint() { return (RouteTransferPoint) this; }

  /** is a Stop specific transfer point (no Trip or Route) */
  default boolean isStopTransferPoint() { return false; }

  default StopTransferPoint asStopTransferPoint() { return (StopTransferPoint) this; }

  /** is a Station specific transfer point (no Trip or Route) */
  default boolean isStationTransferPoint() { return false; }

  default StationTransferPoint asStationTransferPoint() { return (StationTransferPoint) this; }


  /**
   * Utility method witch can be used in APIs to get the trip, if it exists, from a transfer point.
   */
  @Nullable
  static Trip getTrip(TransferPoint point) {
    return point.isTripTransferPoint() ? point.asTripTransferPoint().getTrip() : null;
  }

  /**
   * Utility method witch can be used in APIs to get the route, if it exists, from a transfer point.
   */
  @Nullable
  static Route getRoute(TransferPoint point) {
    if(point.isTripTransferPoint()) {
      return point.asTripTransferPoint().getTrip().getRoute();
    }
    if(point.isRouteTransferPoint()) {
      return point.asRouteTransferPoint().getRoute();
    }
    return null;
  }
}
