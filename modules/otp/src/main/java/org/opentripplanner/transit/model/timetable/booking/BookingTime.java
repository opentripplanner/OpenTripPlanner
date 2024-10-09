package org.opentripplanner.transit.model.timetable.booking;

import java.io.Serializable;
import java.time.LocalTime;
import java.util.Objects;
import org.opentripplanner.framework.time.TimeUtils;

/**
 * Represents either the earliest or latest time a trip can be booked relative to the departure day
 * of the trip.
 */
public class BookingTime implements Serializable {

  private final LocalTime time;

  private final int daysPrior;

  public BookingTime(LocalTime time, int daysPrior) {
    this.time = time;
    this.daysPrior = daysPrior;
  }

  public LocalTime getTime() {
    return time;
  }

  public int getDaysPrior() {
    return daysPrior;
  }

  /**
   * Get the relative time of day, can be negative if the {@code daysPrior} is set. This method
   * does account for DST changes within the relative time.
   */
  public int relativeTimeSeconds() {
    return time.toSecondOfDay() - daysPrior * TimeUtils.ONE_DAY_SECONDS;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BookingTime that = (BookingTime) o;
    return daysPrior == that.daysPrior && Objects.equals(time, that.time);
  }

  @Override
  public int hashCode() {
    return Objects.hash(time, daysPrior);
  }

  @Override
  public String toString() {
    return daysPrior == 0 ? time.toString() : time.toString() + "-" + daysPrior + "d";
  }
}
