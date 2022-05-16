package org.opentripplanner.model.calendar.openinghours;

import java.time.ZoneId;
import java.util.List;
import org.opentripplanner.util.lang.ToStringBuilder;

public class OHCalendar {

  private final ZoneId zoneId;
  private final List<OpeningHours> openingHours;

  public OHCalendar(ZoneId zoneId, List<OpeningHours> openingHours) {
    this.zoneId = zoneId;
    this.openingHours = openingHours;
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(OHCalendar.class)
      .addObj("zoneId", zoneId)
      .addCol("openingHours", openingHours)
      .toString();
  }
}
