package org.opentripplanner.gtfs.mapping;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.model.calendar.ServiceCalendarDate;
import org.opentripplanner.utils.collection.MapUtils;

/** Responsible for mapping GTFS ServiceCalendarDate into the OTP model. */
class ServiceCalendarDateMapper {

  private final Map<
    org.onebusaway.gtfs.model.ServiceCalendarDate,
    ServiceCalendarDate
  > mappedServiceDates = new HashMap<>();

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
      AgencyAndIdMapper.mapAgencyAndId(rhs.getServiceId()),
      ServiceDateMapper.mapLocalDate(rhs.getDate()),
      rhs.getExceptionType()
    );
  }
}
