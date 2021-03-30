package org.opentripplanner.transit.raptor.rangeraptor.transit;

import org.opentripplanner.model.base.ValueObjectToStringBuilder;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

import java.util.Objects;

/**
 * Board and alight time tuple value object.
 */
public class BoarAndAlightTime {
  private final RaptorTripSchedule trip;
  private final int boardStopPos;
  private final int alightStopPos;

  public BoarAndAlightTime(RaptorTripSchedule trip, int boardStopPos, int alightStopPos) {
    this.trip = trip;
    this.boardStopPos = boardStopPos;
    this.alightStopPos = alightStopPos;
  }

  public int boardTime() {
    return trip.departure(boardStopPos);
  }

  public int alightTime() {
    return trip.arrival(alightStopPos);
  }

  public int boardStopPos() {
    return boardStopPos;
  }

  public int alightStopPos() {
    return alightStopPos;
  }

  @Override
  public String toString() {
    return ValueObjectToStringBuilder
        .of()
        .addLbl("(")
        .addServiceTime(boardTime())
        .addLbl(", ")
        .addServiceTime(alightTime())
        .addLbl(")")
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) { return true; }
    if (o == null || getClass() != o.getClass()) { return false; }
    BoarAndAlightTime that = (BoarAndAlightTime) o;
    return boardStopPos == that.boardStopPos && alightStopPos == that.alightStopPos;
  }

  @Override
  public int hashCode() {
    return Objects.hash(boardStopPos, alightStopPos);
  }
}
