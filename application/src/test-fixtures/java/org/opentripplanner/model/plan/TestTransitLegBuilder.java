package org.opentripplanner.model.plan;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import org.opentripplanner._support.time.ZoneIds;

public class TestTransitLegBuilder {

  private static final LocalDate DATE = LocalDate.parse("2025-11-17");
  private static final ZonedDateTime TIME = LocalTime.parse("12:00")
    .atDate(DATE)
    .atZone(ZoneIds.UTC);
  ZonedDateTime startTime = TIME;
  ZonedDateTime endTime = TIME.plusHours(1);

  public TestTransitLegBuilder withStartTime(String startTime) {
    var time = LocalTime.parse(startTime);
    this.startTime = DATE.atTime(time).atZone(ZoneIds.UTC);
    return this;
  }

  public TestTransitLegBuilder withEndTime(String endTime) {
    var time = LocalTime.parse(endTime);
    this.endTime = DATE.atTime(time).atZone(ZoneIds.UTC);
    return this;
  }

  public TestTransitLeg build() {
    return new TestTransitLeg(this);
  }
}
