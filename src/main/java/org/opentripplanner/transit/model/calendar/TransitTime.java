package org.opentripplanner.transit.model.calendar;

/**
 *
 */
public class TransitTime {

  private final TransitCalendar calendar;
  private final int day;
  private final int time;

  public TransitTime(Builder builder) {
    this.calendar = builder.calendar;
    this.time = builder.time;
    this.day = builder.day;
  }

  public int day() {
    return day;
  }

  public int timeSec() {
    return time;
  }

  public int length() {
    return calendar.dayLengthSeconds(day);
  }

  public static class Builder {

    private final TransitCalendar calendar;
    private int day;
    private int time;

    public Builder(TransitCalendar calendar) {
      this.calendar = calendar;
    }

    public Builder withDay(int day) {
      this.day = day;
      return this;
    }

    public Builder withTime(int time) {
      this.time = time;
      return this;
    }

    public TransitTime build() {
      return new TransitTime(this);
    }
  }
}
