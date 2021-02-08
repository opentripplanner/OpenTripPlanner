package org.opentripplanner.netex.mapping;

import java.time.LocalTime;

public class BookingTime {
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
}
