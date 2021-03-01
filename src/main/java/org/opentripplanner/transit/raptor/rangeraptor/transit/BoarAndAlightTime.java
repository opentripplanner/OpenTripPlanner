package org.opentripplanner.transit.raptor.rangeraptor.transit;

import org.opentripplanner.model.base.ValueObjectToStringBuilder;

import java.util.Objects;

/**
 * Board and alight time tuple value object.
 */
public class BoarAndAlightTime {

  public final int boardTime;
  public final int alightTime;

  BoarAndAlightTime(int boardTime, int alightTime) {
    this.boardTime = boardTime;
    this.alightTime = alightTime;
  }

  @Override
  public String toString() {
    return ValueObjectToStringBuilder
        .of()
        .addLbl("(")
        .addServiceTime(boardTime)
        .addLbl(", ")
        .addServiceTime(alightTime)
        .addLbl(")")
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) { return true; }
    if (o == null || getClass() != o.getClass()) { return false; }
    BoarAndAlightTime that = (BoarAndAlightTime) o;
    return boardTime == that.boardTime && alightTime == that.alightTime;
  }

  @Override
  public int hashCode() {
    return Objects.hash(boardTime, alightTime);
  }
}
