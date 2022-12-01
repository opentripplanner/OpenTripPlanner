package org.opentripplanner.routing.algorithm.raptoradapter.transit.constrainedtransfer;

import javax.annotation.Nullable;
import org.opentripplanner.raptor.spi.RaptorTransferConstraint;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * Encapsulate the information needed to identify a transfer during a Raptor search for a given
 * pattern.
 */
public class TransferForPattern implements Comparable<TransferForPattern> {

  /**
   * Used to filter transfers based on the source-stop-arrival.
   */
  private final TransferPointMatcher sourcePoint;

  /**
   * If {@code null} the constraint apply to all trips
   */
  @Nullable
  private final Trip targetTrip;

  private final RaptorTransferConstraint transferConstraint;
  private final int specificityRanking;

  TransferForPattern(
    TransferPointMatcher sourcePoint,
    @Nullable Trip targetTrip,
    int specificityRanking,
    RaptorTransferConstraint transferConstraint
  ) {
    this.sourcePoint = sourcePoint;
    this.targetTrip = targetTrip;
    this.specificityRanking = specificityRanking;
    this.transferConstraint = transferConstraint;
  }

  public RaptorTransferConstraint getTransferConstraint() {
    return transferConstraint;
  }

  public boolean matchesSourcePoint(int stopIndex, Trip trip) {
    return sourcePoint.match(stopIndex, trip);
  }

  /**
   * A transfer either apply to all target-trips (station-, stop- and route-transfer-points) or to a
   * specific trip (trip-transfer-point).
   */
  public boolean applyToAllTargetTrips() {
    return targetTrip == null;
  }

  /**
   * return {@code true} if this transfer apply to the specified trip, and only that trip.
   *
   * @see #applyToAllTargetTrips()
   */
  public boolean applyToTargetTrip(Trip targetTrip) {
    return this.targetTrip == targetTrip;
  }

  /**
   * Transfers should be sorted after the specificityRanking, this make sure the transfer with the
   * highest ranking is used by raptor.
   */
  @Override
  public int compareTo(TransferForPattern o) {
    return o.specificityRanking - specificityRanking;
  }
}
