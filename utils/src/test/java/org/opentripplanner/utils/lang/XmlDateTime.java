package org.opentripplanner.utils.lang;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class XmlDateTime {

  private ZonedDateTime zonedDateTime;
  private LocalDateTime localDateTime;

  public XmlDateTime(ZonedDateTime zonedDateTime) {
    this.zonedDateTime = zonedDateTime;
  }

  public XmlDateTime(LocalDateTime localDateTime) {
    this.localDateTime = localDateTime;
  }

  public ZonedDateTime atZone(ZoneId zoneId) {
    return zonedDateTime != null ? zonedDateTime.withZoneSameInstant(zoneId) : localDateTime.atZone(zoneId);
  };

}
