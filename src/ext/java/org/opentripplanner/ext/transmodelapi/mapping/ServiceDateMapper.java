package org.opentripplanner.ext.transmodelapi.mapping;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import org.opentripplanner.model.calendar.ServiceDate;

public class ServiceDateMapper {

  private final ZoneId timeZone;

  public ServiceDateMapper(ZoneId timeZone) {
    this.timeZone = timeZone;
  }

  public Long serviceDateToSecondsSinceEpoch(ServiceDate serviceDate) {
    if (serviceDate == null) {
      return null;
    }

    return LocalDate
      .of(serviceDate.getYear(), serviceDate.getMonth(), serviceDate.getDay())
      .atStartOfDay(timeZone)
      .toEpochSecond();
  }

  public ServiceDate secondsSinceEpochToServiceDate(Long secondsSinceEpoch) {
    if (secondsSinceEpoch == null) {
      return new ServiceDate(timeZone);
    }
    return new ServiceDate(Instant.ofEpochSecond(secondsSinceEpoch).atZone(timeZone).toLocalDate());
  }
}
