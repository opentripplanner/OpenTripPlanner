package org.opentripplanner.model.transfer;

import java.io.Serializable;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.utils.tostring.ValueObjectToStringBuilder;

public final class TripTransferPoint implements TransferPoint, Serializable {

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
  public int getSpecificityRanking() {
    return 4;
  }

  @Override
  public boolean isTripTransferPoint() {
    return true;
  }

  @Override
  public String toString() {
    return ValueObjectToStringBuilder.of()
      .addText("TripTP{")
      .addObj(trip.getId())
      .addText(", stopPos ")
      .addNum(stopPositionInPattern)
      .addText("}")
      .toString();
  }
}
