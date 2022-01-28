package org.opentripplanner.model.transfer;

import java.io.Serializable;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.base.ValueObjectToStringBuilder;

public final class TripTransferPoint implements TransferPoint, Serializable {

  private static final long serialVersionUID = 1L;

  private final Trip trip;
  private final int stopPositionInPattern;


  public TripTransferPoint(Trip trip, int stopPositionInPattern) {
    this.trip = trip;
    this.stopPositionInPattern = stopPositionInPattern;
  }

  public Trip getTrip() {
    return trip;
  }

  public int getStopPositionInPattern() {
    return stopPositionInPattern;
  }

  @Override
  public boolean appliesToAllTrips() {
    return false;
  }

  @Override
  public int getSpecificityRanking() { return 4; }

  @Override
  public boolean isTripTransferPoint() { return true; }

  @Override
  public String toString() {
    return ValueObjectToStringBuilder.of()
            .addText("<Trip ")
            .addObj(trip.getId())
            .addText(", stopPos ")
            .addNum(stopPositionInPattern)
            .addText(">")
            .toString();
  }
}
