package org.opentripplanner.ext.transmodelapi.mapping;

import java.time.LocalDate;
import java.util.Date;
import java.util.TimeZone;
import org.opentripplanner.model.calendar.ServiceDate;

public class ServiceDateMapper {

  private final TimeZone timeZone;

  public ServiceDateMapper(TimeZone timeZone) {
    this.timeZone = timeZone;
  }

  public Long serviceDateToSecondsSinceEpoch(ServiceDate serviceDate) {
    if (serviceDate == null) {
      return null;
    }

    return LocalDate
      .of(serviceDate.getYear(), serviceDate.getMonth(), serviceDate.getDay())
      .atStartOfDay(timeZone.toZoneId())
      .toEpochSecond();
  }

  public ServiceDate secondsSinceEpochToServiceDate(Long secondsSinceEpoch) {
    if (secondsSinceEpoch == null) {
      return new ServiceDate();
    }
    return new ServiceDate(new Date(secondsSinceEpoch * 1000));
  }
}
