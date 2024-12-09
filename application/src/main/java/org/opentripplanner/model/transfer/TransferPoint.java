package org.opentripplanner.model.transfer;

import javax.annotation.Nullable;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.timetable.Trip;

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
 *     {@link StationTransferPoint} This applies to all trips stopping at a stop part of the given
 *     station.
 *     <p>The specificity-ranking is above {@link StationTransferPoint}s and less than
 *     {@link RouteStationTransferPoint}.
 *   </li>
 *   <li>
 *     A {@link RouteStationTransferPoint} is a from/to point for a Route at the given stop. This
 *     only exists in GTFS, not in the Nordic NeTex profile.
 *
 *     <p>The specificity-ranking is above {@link StopTransferPoint}s and less than
 *     {@link RouteStopTransferPoint}.
 *   </li>
 *   <li>
 *     A {@link RouteStopTransferPoint} is a from/to point for a Route at the given station. This
 *     only exists in GTFS, not in the Nordic NeTex profile.
 *
 *     <p>The specificity-ranking is above {@link RouteStationTransferPoint}s and less than
 *     {@link TripTransferPoint}.
 *   </li>
 *   <li>
 *     {@link TripTransferPoint} A transfer from/to a Trip at the given stop position(not stop).
 *     The GTFS Transfers may specify a transfer from/to a trip and stop/station. But in OTP we
 *     map the stop to a stop position in pattern. The OTP model {@link TripTransferPoint} does NOT
 *     reference the stop/station, but the {@code stopPositionInPattern} instead. There is two
 *     reasons for this. In NeTEx the an interchange is from a trip and stop-point, so this model
 *     fits better with NeTEx. The second reason is that real-time updates could invalidate the
 *     trip-transfer-point, since the stop could change to another platform(common for railway
 *     stations). To account for this the RT-update would need to patch the trip-transfer-point.
 *     We simplify the RT-updates by converting the stop to a stop-position-in-pattern.
 * <p>
 *     This is the most specific point type.
 *   </li>
 * </ol>
 * <p>
 */
public interface TransferPoint {
  /**
   * Utility method which can be used in APIs to get the trip, if it exists, from a transfer point.
   */
  @Nullable
  static Trip getTrip(TransferPoint point) {
    return point.isTripTransferPoint() ? point.asTripTransferPoint().getTrip() : null;
  }

  /**
   * Utility method which can be used in APIs to get the route, if it exists, from a transfer
   * point.
   */
  @Nullable
  static Route getRoute(TransferPoint point) {
    if (point.isTripTransferPoint()) {
      return point.asTripTransferPoint().getTrip().getRoute();
    }
    if (point.isRouteStopTransferPoint()) {
      return point.asRouteStopTransferPoint().getRoute();
    }
    if (point.isRouteStationTransferPoint()) {
      return point.asRouteStationTransferPoint().getRoute();
    }
    return null;
  }

  /** Return {@code true} if this transfer point apply to all trips in pattern */
  boolean appliesToAllTrips();

  /**
   * <a href="https://developers.google.com/transit/gtfs/reference/gtfs-extensions#specificity-of-a-transfer">
   * Specificity of a transfer
   * </a>
   */
  int getSpecificityRanking();

  /** is a Trip specific transfer point */
  default boolean isTripTransferPoint() {
    return false;
  }

  default TripTransferPoint asTripTransferPoint() {
    return (TripTransferPoint) this;
  }

  /** is a Route specific transfer point */
  default boolean isRouteStationTransferPoint() {
    return false;
  }

  default RouteStationTransferPoint asRouteStationTransferPoint() {
    return (RouteStationTransferPoint) this;
  }

  /** is a Route specific transfer point */
  default boolean isRouteStopTransferPoint() {
    return false;
  }

  default RouteStopTransferPoint asRouteStopTransferPoint() {
    return (RouteStopTransferPoint) this;
  }

  /** is a Stop specific transfer point (no Trip or Route) */
  default boolean isStopTransferPoint() {
    return false;
  }

  default StopTransferPoint asStopTransferPoint() {
    return (StopTransferPoint) this;
  }

  /** is a Station specific transfer point (no Trip or Route) */
  default boolean isStationTransferPoint() {
    return false;
  }

  default StationTransferPoint asStationTransferPoint() {
    return (StationTransferPoint) this;
  }
}
