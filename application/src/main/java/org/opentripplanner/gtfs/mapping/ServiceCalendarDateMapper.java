package org.opentripplanner.gtfs.mapping;

import java.util.Collection;
import org.onebusaway.gtfs.model.ServiceCalendarDate;
import org.opentripplanner.transit.model.calendar.TripCalendarsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Responsible for mapping GTFS ServiceCalendarDate rows into the OTP model. */
class ServiceCalendarDateMapper {

  private static final Logger LOG = LoggerFactory.getLogger(ServiceCalendarDateMapper.class);

  /** GTFS calendar_dates.txt exception_type: service has been added for the date. */
  private static final int EXCEPTION_TYPE_ADD = 1;

  /** GTFS calendar_dates.txt exception_type: service has been removed for the date. */
  private static final int EXCEPTION_TYPE_REMOVE = 2;

  private final IdFactory idFactory;

  ServiceCalendarDateMapper(IdFactory idFactory) {
    this.idFactory = idFactory;
  }

  void map(Collection<ServiceCalendarDate> allServiceDates, TripCalendarsBuilder calendars) {
    if (allServiceDates == null) {
      return;
    }
    allServiceDates.forEach(d -> map(d, calendars));
  }

  /** Map from GTFS to OTP model, {@code null} safe. */
  void map(ServiceCalendarDate rhs, TripCalendarsBuilder calendars) {
    if (rhs == null) {
      return;
    }
    var serviceId = idFactory.createId(rhs.getServiceId(), "calendar date");
    var date = ServiceDateMapper.mapLocalDate(rhs.getDate());
    switch (rhs.getExceptionType()) {
      case EXCEPTION_TYPE_ADD -> calendars.addServiceDate(serviceId, date);
      case EXCEPTION_TYPE_REMOVE -> calendars.removeServiceDate(serviceId, date);
      default -> LOG.warn(
        "Unknown CalendarDate exception type: {}, service id: {}",
        rhs.getExceptionType(),
        serviceId
      );
    }
  }
}
