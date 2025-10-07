package org.opentripplanner.model;

import java.io.Serializable;
import java.util.Objects;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public final class Frequency implements Serializable {

  private final Trip trip;
  private final int startTime;
  private final int endTime;
  private final int headwaySecs;
  private final boolean exactTimes;

  public Frequency(Trip trip, int startTime, int endTime, int headwaySecs, boolean exactTimes) {
    this.trip = trip;
    this.startTime = startTime;
    this.endTime = endTime;
    this.headwaySecs = headwaySecs;
    this.exactTimes = exactTimes;
  }

  public Trip trip() {
    return trip;
  }

  public int startTime() {
    return startTime;
  }

  public int endTime() {
    return endTime;
  }

  public int headwaySecs() {
    return headwaySecs;
  }

  public boolean exactTimes() {
    return exactTimes;
  }

  @Override
  public int hashCode() {
    return Objects.hash(trip, startTime, endTime, headwaySecs, exactTimes);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Frequency frequency = (Frequency) o;
    return (
      startTime == frequency.startTime &&
      endTime == frequency.endTime &&
      headwaySecs == frequency.headwaySecs &&
      exactTimes == frequency.exactTimes &&
      Objects.equals(trip, frequency.trip)
    );
  }

  public String toString() {
    return ToStringBuilder.of(Frequency.class)
      .addObjOp("trip", trip, AbstractTransitEntity::getId)
      .addServiceTime("start", startTime)
      .addServiceTime("end", endTime)
      .toString();
  }
}
