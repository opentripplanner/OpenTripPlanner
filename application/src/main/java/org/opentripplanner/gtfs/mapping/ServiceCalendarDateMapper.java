package org.opentripplanner.gtfs.mapping;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.model.calendar.ServiceCalendarDate;
import org.opentripplanner.utils.collection.MapUtils;

/** Responsible for mapping GTFS ServiceCalendarDate into the OTP model. */
class ServiceCalendarDateMapper {

  private final IdFactory idFactory;

  private final Map<
    org.onebusaway.gtfs.model.ServiceCalendarDate,
    ServiceCalendarDate
  > mappedServiceDates = new HashMap<>();

  ServiceCalendarDateMapper(IdFactory idFactory) {
    this.idFactory = idFactory;
  }

  Collection<ServiceCalendarDate> map(
    Collection<org.onebusaway.gtfs.model.ServiceCalendarDate> allServiceDates
  ) {
    return MapUtils.mapToList(allServiceDates, this::map);
  }

  /** Map from GTFS to OTP model, {@code null} safe. */
  ServiceCalendarDate map(org.onebusaway.gtfs.model.ServiceCalendarDate orginal) {
    return orginal == null ? null : mappedServiceDates.computeIfAbsent(orginal, this::doMap);
  }

  private ServiceCalendarDate doMap(org.onebusaway.gtfs.model.ServiceCalendarDate rhs) {
    return new ServiceCalendarDate(
      idFactory.createId(rhs.getServiceId(), "calendar date"),
      ServiceDateMapper.mapLocalDate(rhs.getDate()),
      rhs.getExceptionType()
    );
  }
}
