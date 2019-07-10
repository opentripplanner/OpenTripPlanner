package org.opentripplanner.routing.algorithm.raptor.transit.mappers;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.model.CalendarService;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.calendar.ServiceDate;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This uses the integer service codes that are already present in the pre-Raptor OTP graph.
 */
class ServiceCalendarMapper {
    static Multimap<LocalDate, Integer> mapServiceCodesByLocalDates(
            CalendarService calendarService,
            Map<FeedScopedId, Integer> serviceCodes
    ) {
        Multimap<LocalDate, Integer> serviceCodesByLocalDates = HashMultimap.create();


        if (calendarService != null) {
            for (FeedScopedId serviceId : calendarService.getServiceIds()) {
                Set<LocalDate> localDates = calendarService.getServiceDatesForServiceId(serviceId)
                        .stream()
                        .map(ServiceCalendarMapper::localDateFromServiceDate)
                        .collect(Collectors.toSet());

                int serviceIndex = serviceCodes.get(serviceId);

                for (LocalDate date : localDates) {
                    serviceCodesByLocalDates.put(date, serviceIndex);
                }
            }
        }
        return serviceCodesByLocalDates;
    }

    public static LocalDate localDateFromServiceDate(ServiceDate serviceDate) {
        return LocalDate.of(serviceDate.getYear(), serviceDate.getMonth(), serviceDate.getDay());
    }
}
