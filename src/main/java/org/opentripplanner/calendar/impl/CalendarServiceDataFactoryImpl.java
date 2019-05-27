/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.calendar.impl;

import org.opentripplanner.model.Agency;
import org.opentripplanner.model.CalendarService;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.ServiceCalendar;
import org.opentripplanner.model.ServiceCalendarDate;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.calendar.LocalizedServiceId;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import static java.util.stream.Collectors.groupingBy;

/**
 * We perform initial date calculations in the timezone of the host jvm, which
 * may be different than the timezone of an agency with the specified service
 * id. To my knowledge, the calculation should work the same, which is to say I
 * can't immediately think of any cases where the service dates would be
 * computed incorrectly.
 *
 * @author bdferris
 */
public class CalendarServiceDataFactoryImpl {

    private static final Logger LOG = LoggerFactory.getLogger(CalendarServiceDataFactoryImpl.class);

    private final List<Agency> agencies;
    private final Map<FeedScopedId, List<ServiceCalendarDate>> calendarDatesByServiceId;
    private final Map<FeedScopedId, List<ServiceCalendar>> calendarsByServiceId;
    private final Map<FeedScopedId, List<String>> tripAgencyIdsByServiceId;
    private final Set<FeedScopedId> serviceIds;

    public static CalendarService createCalendarService(OtpTransitServiceBuilder transitDaoBuilder) {
        return new CalendarServiceImpl(createCalendarServiceData(transitDaoBuilder));
    }

    public static CalendarServiceData createCalendarServiceData(OtpTransitServiceBuilder transitDaoBuilder) {
        return new CalendarServiceDataFactoryImpl(transitDaoBuilder).createData();
    }

    public static CalendarServiceData createCalendarSrvDataWithoutDatesForLocalizedSrvId(
            OtpTransitServiceBuilder transitDaoBuilder
    ) {
        return (new CalendarServiceDataFactoryImpl(transitDaoBuilder) {
            @Override void addDatesForLocalizedServiceId(
                    FeedScopedId serviceId, List<ServiceDate> serviceDates, CalendarServiceData data
            ) {
                // Skip creation of datesForLocalizedServiceId
            }
        }).createData();
    }

    private CalendarServiceDataFactoryImpl(OtpTransitServiceBuilder transitBuilder) {
        agencies = new ArrayList<>(transitBuilder.getAgenciesById().values());
        calendarDatesByServiceId = transitBuilder.getCalendarDates()
                .stream()
                .collect(groupingBy(ServiceCalendarDate::getServiceId));
        calendarsByServiceId = transitBuilder.getCalendars()
                .stream()
                .collect(groupingBy(ServiceCalendar::getServiceId));
        serviceIds = merge(calendarDatesByServiceId.keySet(), calendarsByServiceId.keySet());

        tripAgencyIdsByServiceId = createTripAgencyIdByServiceIdMap(transitBuilder.getTripsById().values());
    }

    CalendarServiceData createData() {

        CalendarServiceData data = new CalendarServiceData();

        setTimeZonesForAgencies(data);

        int index = 0;

        for (FeedScopedId serviceId : serviceIds) {

            index++;

            LOG.debug("serviceId=" + serviceId + " (" + index + "/" + serviceIds.size() + ")");

            TimeZone serviceIdTimeZone = data.getTimeZoneForAgencyId(serviceId.getAgencyId());
            if (serviceIdTimeZone == null) {
                serviceIdTimeZone = TimeZone.getDefault();
            }

            Set<ServiceDate> activeDates = getServiceDatesForServiceId(serviceId,
                    serviceIdTimeZone);

            List<ServiceDate> serviceDates = new ArrayList<>(activeDates);
            Collections.sort(serviceDates);

            data.putServiceDatesForServiceId(serviceId, serviceDates);

            addDatesForLocalizedServiceId(serviceId, serviceDates, data);
        }

        return data;
    }

    void addDatesForLocalizedServiceId (
            FeedScopedId serviceId, List<ServiceDate> serviceDates, CalendarServiceData data) {
        List<String> tripAgencyIds = tripAgencyIdsByServiceId.get(serviceId);

        if(tripAgencyIds == null) {
            LOG.warn("There is no trip with service id '{}'. No index for Localized service dates can be created.", serviceId);
            return;
        }

        Set<TimeZone> timeZones = new HashSet<>();
        for (String tripAgencyId : tripAgencyIds) {
            TimeZone timeZone = data.getTimeZoneForAgencyId(tripAgencyId);
            timeZones.add(timeZone);
        }

        for (TimeZone timeZone : timeZones) {

            List<Date> dates = new ArrayList<>(serviceDates.size());
            for (ServiceDate serviceDate : serviceDates)
                dates.add(serviceDate.getAsDate(timeZone));

            LocalizedServiceId id = new LocalizedServiceId(serviceId, timeZone);
            data.putDatesForLocalizedServiceId(id, dates);
        }
    }

    private Set<ServiceDate> getServiceDatesForServiceId(FeedScopedId serviceId,
            TimeZone serviceIdTimeZone) {
        Set<ServiceDate> activeDates = new HashSet<>();
        ServiceCalendar c = findCalendarForServiceId(serviceId);

        if (c != null) {
            addDatesFromCalendar(c, serviceIdTimeZone, activeDates);
        }
        List<ServiceCalendarDate> dates = calendarDatesByServiceId.get(serviceId);
        if(dates != null) {
            for (ServiceCalendarDate cd : dates) {
                addAndRemoveDatesFromCalendarDate(cd, activeDates);
            }
        }
        return activeDates;
    }

    private ServiceCalendar findCalendarForServiceId(FeedScopedId serviceId) {
        List<ServiceCalendar> calendars = calendarsByServiceId.get(serviceId);

        if(calendars == null || calendars.isEmpty()) {
            return null;
        }
        if(calendars.size() == 1) {
            return calendars.get(0);
        }
        throw new MultipleCalendarsForServiceIdException(serviceId);
    }

    private void setTimeZonesForAgencies(CalendarServiceData data) {
        for (Agency agency : agencies) {
            TimeZone timeZone = TimeZone.getTimeZone(agency.getTimezone());
            if (timeZone.getID().equals("GMT") && !agency.getTimezone().toUpperCase()
                    .equals("GMT")) {
                throw new UnknownAgencyTimezoneException(agency.getName(), agency.getTimezone());
            }
            data.putTimeZoneForAgencyId(agency.getId(), timeZone);
        }
    }

    private void addDatesFromCalendar(ServiceCalendar calendar, TimeZone timeZone,
            Set<ServiceDate> activeDates) {

        // We calculate service dates relative to noon so as to avoid any weirdness
        // relative to DST.
        Date startDate = getServiceDateAsNoon(calendar.getStartDate(), timeZone);
        Date endDate = getServiceDateAsNoon(calendar.getEndDate(), timeZone);

        java.util.Calendar c = java.util.Calendar.getInstance(timeZone);
        c.setTime(startDate);

        while (true) {
            Date date = c.getTime();
            if (date.after(endDate))
                break;

            int day = c.get(java.util.Calendar.DAY_OF_WEEK);
            boolean active = false;

            switch (day) {
            case java.util.Calendar.MONDAY:
                active = calendar.getMonday() == 1;
                break;
            case java.util.Calendar.TUESDAY:
                active = calendar.getTuesday() == 1;
                break;
            case java.util.Calendar.WEDNESDAY:
                active = calendar.getWednesday() == 1;
                break;
            case java.util.Calendar.THURSDAY:
                active = calendar.getThursday() == 1;
                break;
            case java.util.Calendar.FRIDAY:
                active = calendar.getFriday() == 1;
                break;
            case java.util.Calendar.SATURDAY:
                active = calendar.getSaturday() == 1;
                break;
            case java.util.Calendar.SUNDAY:
                active = calendar.getSunday() == 1;
                break;
            }

            if (active) {
                addServiceDate(activeDates, new ServiceDate(c));
            }

            c.add(java.util.Calendar.DAY_OF_YEAR, 1);
        }
    }

    private void addAndRemoveDatesFromCalendarDate(ServiceCalendarDate calendarDate,
            Set<ServiceDate> activeDates) {
        ServiceDate serviceDate = calendarDate.getDate();
        Date targetDate = calendarDate.getDate().getAsDate();
        Calendar c = Calendar.getInstance();
        c.setTime(targetDate);

        switch (calendarDate.getExceptionType()) {
        case ServiceCalendarDate.EXCEPTION_TYPE_ADD:
            addServiceDate(activeDates, serviceDate);
            break;
        case ServiceCalendarDate.EXCEPTION_TYPE_REMOVE:
            activeDates.remove(serviceDate);
            break;
        default:
            LOG.warn("unknown CalendarDate exception type: " + calendarDate.getExceptionType());
            break;
        }
    }

    private void addServiceDate(Set<ServiceDate> activeDates, ServiceDate serviceDate) {
        activeDates.add(new ServiceDate(serviceDate));
    }

    private static Date getServiceDateAsNoon(ServiceDate serviceDate, TimeZone timeZone) {
        Calendar c = serviceDate.getAsCalendar(timeZone);
        c.add(Calendar.HOUR_OF_DAY, 12);
        return c.getTime();
    }

    private static Map<FeedScopedId, List<String>> createTripAgencyIdByServiceIdMap(Collection<Trip> trips) {
        Map<FeedScopedId, Set<String>> agencyIdsByServiceIds = new HashMap<>();

        for (Trip t : trips) {
            // In GFTS providing an agency is optional, but we add an empty set
            // anyway to make sure all trips are represented in the map.
            Collection<String> agencyIds = agencyIdsByServiceIds
                    .computeIfAbsent(t.getServiceId(), key -> new HashSet<>());

            if(t.getRoute().getAgency() != null) {
                agencyIds.add(t.getRoute().getAgency().getId());
            }
        }

        Map<FeedScopedId, List<String>> map = new HashMap<>();

        for (Map.Entry<FeedScopedId, Set<String>> entry : agencyIdsByServiceIds.entrySet()) {
            FeedScopedId tripServiceId = entry.getKey();
            List<String> agencyIds = new ArrayList<>(entry.getValue());
            Collections.sort(agencyIds);
            map.put(tripServiceId, agencyIds);
        }
        return map;
    }

    static <T> Set<T> merge(Collection<T> set1, Collection<T> set2) {
        Set<T> newSet = new HashSet<>();
        newSet.addAll(set1);
        newSet.addAll(set2);
        return newSet;
    }
}