package org.opentripplanner.model.transfer;

import java.io.Serializable;
import java.util.Objects;
import org.opentripplanner.model.Trip;

public class TripTransferPoint implements TransferPoint, Serializable {

  private static final long serialVersionUID = 1L;

  private final Trip trip;
  private final int stopPosition;


  public TripTransferPoint(Trip trip, int stopPosition) {
    this.trip = trip;
    this.stopPosition = stopPosition;
  }

  @Override
  public final Trip getTrip() {
    return trip;
  }

  @Override
  public final int getStopPosition() {
    return stopPosition;
  }

  /**
   * <a href="https://developers.google.com/transit/gtfs/reference/gtfs-extensions#specificity-of-a-transfer">
   *     GTFS Specificity of a transfer
   * </a>
   * {@link #equals(Object)}
   */
  @Override
  public int getSpecificityRanking() { return 2; }

  @Override
  public String toString() {
    return "(trip: " + trip.getId() + ", stopPos: " + stopPosition + ")";
  }

  /**
   * This equals is intentionally final and enforce equality based on the *trip* and
   * *stop-position*. Any sub-type is equal if the trip and stop-position match, the type is not
   * used. This allow us to create sub-types and override the {@link #getSpecificityRanking()}.
   */
  @Override
  public final boolean equals(Object o) {
    if (this == o) { return true; }
    if (!(o instanceof TripTransferPoint)) { return false; }

    TripTransferPoint that = (TripTransferPoint) o;
    return stopPosition == that.stopPosition && trip.getId().equals(that.trip.getId());
  }

  @Override
  public final int hashCode() {
    return Objects.hash(trip.getId(), stopPosition);
  }
}
