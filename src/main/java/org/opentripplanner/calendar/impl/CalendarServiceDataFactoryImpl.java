/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.calendar.impl;

import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.ServiceCalendar;
import org.opentripplanner.model.ServiceCalendarDate;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.calendar.LocalizedServiceId;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.model.CalendarService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

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

    private final OtpTransitService transitService;

    public static CalendarService createCalendarService(OtpTransitService transitService) {
        return new CalendarServiceImpl(createCalendarServiceData(transitService));
    }

    public static CalendarServiceData createCalendarServiceData(OtpTransitService transitService) {
        return new CalendarServiceDataFactoryImpl(transitService).createData();
    }

    public static CalendarServiceData createCalendarSrvDataWithoutDatesForLocalizedSrvId(OtpTransitService transitService) {
        return (new CalendarServiceDataFactoryImpl(transitService) {
            @Override void addDatesForLocalizedServiceId(
                    FeedScopedId serviceId, List<ServiceDate> serviceDates, CalendarServiceData data
            ) {
                // Skip creation of datesForLocalizedServiceId
            }
        }).createData();
    }

    private CalendarServiceDataFactoryImpl(OtpTransitService transitService) {
        this.transitService = transitService;
    }

    CalendarServiceData createData() {

        CalendarServiceData data = new CalendarServiceData();

        setTimeZonesForAgencies(data);

        List<FeedScopedId> serviceIds = transitService.getAllServiceIds();

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
        List<String> tripAgencyIds = transitService.getTripAgencyIdsReferencingServiceId(serviceId);
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
        ServiceCalendar c = transitService.getCalendarForServiceId(serviceId);

        if (c != null) {
            addDatesFromCalendar(c, serviceIdTimeZone, activeDates);
        }
        for (ServiceCalendarDate cd : transitService.getCalendarDatesForServiceId(serviceId)) {
            addAndRemoveDatesFromCalendarDate(cd, activeDates);
        }
        return activeDates;
    }

    private void setTimeZonesForAgencies(CalendarServiceData data) {
        for (Agency agency : transitService.getAllAgencies()) {
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
}