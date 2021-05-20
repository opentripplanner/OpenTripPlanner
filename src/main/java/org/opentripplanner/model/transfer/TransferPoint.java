package org.opentripplanner.model.transfer;

import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;


/**
 * This interface is used to represent a point or location where a transfer start from or end.
 *
 * <p>There are 3 different Transfer points:
 * <ol>
 *   <li>
 *     {@link StopTransferPoint} This apply to all trip stopping at the given stop.
 *
 *     <p>This is the least specific type, and is overridden if a more specific type exist.
 *   </li>
 *   <li>
 *     A {@link RouteTransferPoint} is a from/to point for a Route at the given stop. This only
 *     exist in GTFS, not in the Nordic NeTex profile. To support this we expand the route into
 *     all trips defined for it, and create {@link RouteTransferPoint} for each trip. We do the
 *     expansion because a Route may have more than on TripPattern and we want to use the stop
 *     position in pattern, not the stop for matching actual transfers. The reason is that
 *     real-time updates could invalidate a (route+stop)-transfer-point, since the stop could
 *     change to another platform(very common for railway stations). To account for this the
 *     RT-update would have to patch the (route&stop)-transfer-point. We simplify the RT-updates
 *     by converting expanding (route+stop) to (trip+stop position).
 *
 *     <p>The specificity-ranking is above {@link StopTransferPoint}s and less than
 *     {@link TripTransferPoint}.
 *   </li>
 *   <li>
 *     {@link TripTransferPoint} A transfer from/to a Trip at the given stop position(not stop).
 *     GTFS Transfers specify a transfer from/to a trip and stop. But in OTP we map the stop to a
 *     stop position in pattern instead. This make sure that the transfer is still valid after a
 *     real-time update where the stop is changed. Especially for train stations changing the
 *     train platform is common and by using the stop position in pattern not the stop this
 *     become more robust. So, the OTP implementation follow the NeTEx Interchange definition
 *     here, not the GTFS specification.
 *
 *     <p>This is the most specific point type, and will override both {@link RouteTransferPoint}
 *     and {@link StopTransferPoint} if more than one match exist.
 *   </li>
 * </ol>
 */
public interface TransferPoint {

  int NOT_AVAILABLE = -1;

  default Stop getStop() {
    return null;
  }

  default Trip getTrip() {
    return null;
  }

  /**
   * If the given transfer point is a {@link TripTransferPoint}, this method return the stop
   * position in the trip pattern. If this transfer point is just a stop or a stop+rout this method
   * return {@link #NOT_AVAILABLE}.
   */
  default int getStopPosition() {
    return NOT_AVAILABLE;
  }

  /**
   * <a href="https://developers.google.com/transit/gtfs/reference/gtfs-extensions#specificity-of-a-transfer">
   * Specificity of a transfer
   * </a>
   */
  int getSpecificityRanking();

  default boolean matches(Trip trip, int stopPos) {
    // Note! We use "==" here since there should not be duplicate instances of trips
    return getStopPosition() == stopPos && getTrip() == trip;
  }
}
