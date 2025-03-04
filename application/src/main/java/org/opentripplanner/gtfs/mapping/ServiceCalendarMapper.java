package org.opentripplanner.gtfs.mapping;

import static org.opentripplanner.gtfs.mapping.AgencyAndIdMapper.mapAgencyAndId;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.model.calendar.ServiceCalendar;
import org.opentripplanner.utils.collection.MapUtils;

/** Responsible for mapping GTFS ServiceCalendar into the OTP model. */
class ServiceCalendarMapper {

  private final Map<org.onebusaway.gtfs.model.ServiceCalendar, ServiceCalendar> mappedCalendars =
    new HashMap<>();

  Collection<ServiceCalendar> map(
    Collection<org.onebusaway.gtfs.model.ServiceCalendar> allServiceCalendars
  ) {
    return MapUtils.mapToList(allServiceCalendars, this::map);
  }

  /** Map from GTFS to OTP model, {@code null} safe. */
  ServiceCalendar map(org.onebusaway.gtfs.model.ServiceCalendar orginal) {
    return orginal == null ? null : mappedCalendars.computeIfAbsent(orginal, this::doMap);
  }

  private ServiceCalendar doMap(org.onebusaway.gtfs.model.ServiceCalendar rhs) {
    ServiceCalendar lhs = new ServiceCalendar();

    lhs.setServiceId(mapAgencyAndId(rhs.getServiceId()));
    lhs.setMonday(rhs.getMonday());
    lhs.setTuesday(rhs.getTuesday());
    lhs.setWednesday(rhs.getWednesday());
    lhs.setThursday(rhs.getThursday());
    lhs.setFriday(rhs.getFriday());
    lhs.setSaturday(rhs.getSaturday());
    lhs.setSunday(rhs.getSunday());
    lhs.setPeriod(ServiceDateMapper.mapServiceDateInterval(rhs.getStartDate(), rhs.getEndDate()));
    return lhs;
  }
}
